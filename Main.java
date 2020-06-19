import java.io.*;
import java.util.*;

/**
 * The Encoding/Decoding program implements an application that
 * encodes messages, simulates/corrects errors, and decodes messages.
 *
 * @author  KV Le
 * @version 1.0
 * @since   6/15/2020
 */
public class Main {
    /**
     * Runs the Command Line Interface to guide the user through Encoding, Decoding, or
     * Sending a theoretical message
     * @param args - No use in this program
     */
    public static void main(String[] args) {
        System.out.print("Error Correcting Encoder/Decoder by KV Le");
        boolean formatting = true;
        String format = "";
        while (formatting) {
            System.out.print("Please choose a proper Encoding/Decoding format " +
                    "(H)amming, (D)ouble-Bit Encoding: ");
            format = new Scanner(System.in).nextLine().trim().toLowerCase();
            formatting = !("h".equals(format) || "d".equals(format));
        }
        System.out.println();
        EncoderDecoder encoderDecoder;
        boolean running = true;

        while (running) {
            System.out.println("Program set to " +
                    ("h".equals(format) ? "Hamming Code" : "Double-Bit") +
                    " Encoding/Decoding");
            System.out.print("Write a mode ((E)ncode, (S)end, (D)ecode)): ");
            String mode = new Scanner(System.in).next().toLowerCase();
            switch (mode) {
                case "e", "encode" -> {
                    encoderDecoder = new EncoderDecoder("./send.txt", format);
                    System.out.println("\nsend.txt:");
                    System.out.println("Text view: " + encoderDecoder);
                    System.out.println("Hexadecimal view: " +
                            encoderDecoder.getHexRepresentation());
                    System.out.println("Binary view: " +
                            encoderDecoder.getBinaryRepresentation());
                    encoderDecoder.encode();
                    System.out.println("\nencoded.txt:");
                    System.out.println("Binary view: " +
                            encoderDecoder.getBinaryRepresentation());
                    System.out.println("Hexadecimal view: " +
                            encoderDecoder.getHexRepresentation());
                }
                case "s", "send" -> {
                    encoderDecoder = new EncoderDecoder("./encoded.txt", format);
                    System.out.println("\nencoded.txt:");
                    System.out.println("Hexadecimal view: " +
                            encoderDecoder.getHexRepresentation());
                    System.out.println("Binary view: " +
                            encoderDecoder.getBinaryRepresentation());
                    encoderDecoder.simulateErrors();
                    System.out.println("\nreceived.txt:");
                    System.out.println("Binary view: " +
                            encoderDecoder.getBinaryRepresentation());
                    System.out.println("Hexadecimal view: " +
                            encoderDecoder.getHexRepresentation());
                }
                case "d", "decode" -> {
                    encoderDecoder = new EncoderDecoder("./received.txt", format);
                    System.out.println("\nreceived.txt:");
                    System.out.println("Hexadecimal view: " +
                            encoderDecoder.getHexRepresentation());
                    System.out.println("Binary view: " +
                            encoderDecoder.getBinaryRepresentation());
                    encoderDecoder.errorCorrect();
                    System.out.println("\ndecoded.txt:");
                    System.out.println("correct: " +
                            encoderDecoder.getBinaryRepresentation());
                    encoderDecoder.decode();
                    System.out.println("decoded: " +
                            encoderDecoder.getBinaryRepresentation());
                    System.out.println("Hexadecimal view: " +
                            encoderDecoder.getHexRepresentation());
                    System.out.println("Text view: " + encoderDecoder);
                }
                case "default" -> System.out.println("\nThis option doesn't exist");
            }
            System.out.println();
            System.out.print("Would you like to end the program? (y/N): ");
            running = !(new Scanner(System.in).nextLine().toLowerCase().trim().equals("y"));
            System.out.println();
        }
        System.out.println("Thanks for using this program :D");
    }
}

/**
 * Object that can retrieve a file and perform Encoding, Decoding, or
 * Error Correction/Simulation
 */
class EncoderDecoder {
    public static final int MAX_BIT_INDEX = 7;
    public static final int BITS_IN_BYTE = 8;

    private final String format;
    private byte[] currentState;

    /**
     * Initializes a new EncoderDecoder Object with the proper Encoding/Decoding format
     * @param filePath - The path to the desired file to be parsed
     * @param format - The Encoding/Decoding Format (Hamming or Double-Bit)
     */
    public EncoderDecoder(String filePath, String format) {
        try (InputStream inputStream = new FileInputStream(filePath)) {
            currentState = inputStream.readAllBytes();
        } catch (FileNotFoundException exception) {
            System.out.println("File wasn't found :(");
        } catch (IOException exception) {
            System.out.println(exception.getMessage());
        }
        this.format = format;
    }

    /**
     * Encodes the current state of bytes to the format of the Encoder
     * It then outputs the new state of bytes to "encoded.txt"
     */
    public void encode() {
        if ("h".equals(format)) {
            encodeHamming();
        } else {
            encodeDoubleBits();
        }
        try (OutputStream outputStream = new FileOutputStream("./encoded.txt")) {
            outputStream.write(currentState);
        } catch (IOException exception) {
            System.out.println("File could not be written to :(");
        }
    }

    /**
     * Error corrects the current state of bytes to the format of the Object
     */
    public void errorCorrect() {
        if ("h".equals(format)) {
            errorCorrectHamming();
        } else {
            errorCorrectDoubleBits();
        }
    }

    /**
     * Decodes the current state of bytes to the format of the Encoder.
     * It then outputs the new state of bytes to "decoded.txt"
     */
    public void decode() {
        if ("h".equals(format)) {
            decodeHamming();
        } else {
            decodeDoubleBits();
        }
        try (OutputStream outputStream = new FileOutputStream("./decoded.txt")) {
            outputStream.write(currentState);
        } catch (IOException exception) {
            System.out.println("File could not be written to :(");
        }
    }

    /**
     * Encodes the current state with Hamming Codes ([7, 4] format specifically)
     * Learn more about them at https://en.wikipedia.org/wiki/Hamming_code
     */
    public void encodeHamming() {
        byte[] encodedBytes = new byte[currentState.length * 2];
        for (int i = 0; i < encodedBytes.length; i++) {
            int[] infoBits = new int[4];
            int idxOgByte = i / 2;
            for (int j = 0; j < 4; j++) {
                int idxBit = (i % 2) * 4 + j;
                infoBits[j] = getBit(currentState[idxOgByte], idxBit);
            }
            // At indexes 0, 1, and 3 are the parity bits for Hamming Codes
            encodedBytes[i] = createByte(new int[]{
                    infoBits[0] ^ infoBits[1] ^ infoBits[3],
                    infoBits[0] ^ infoBits[2] ^ infoBits[3],
                    infoBits[0],
                    infoBits[1] ^ infoBits[2] ^ infoBits[3],
                    infoBits[1],
                    infoBits[2],
                    infoBits[3],
                    0
            });
        }
        currentState = encodedBytes;
    }

    /**
     * Error Corrects the current state with Hamming Codes in mind
     * Learn more about them at https://en.wikipedia.org/wiki/Hamming_code
     */
    public void errorCorrectHamming() {
        /*
        You can find the error bit by combining the parity bits with their corresponding checks and
        comparing with each parity group to find the index of an error and flip it.
        Find out more here: https://www.youtube.com/watch?v=wbH2VxzmoZk
         */
        for (int i = 0; i < currentState.length; i++) {
            currentState[i] = flipBit(currentState[i],
                    ((getBit(currentState[i], 0) ^ getBit(currentState[i], 2) ^
                            getBit(currentState[i], 4) ^ getBit(currentState[i], 6)) +
                            (getBit(currentState[i], 1) ^ getBit(currentState[i], 2) ^
                                    getBit(currentState[i], 5) ^ getBit(currentState[i], 6)) * 2 +
                            (getBit(currentState[i], 3) ^ getBit(currentState[i], 4) ^
                                    getBit(currentState[i], 5) ^ getBit(currentState[i], 6)) * 4) - 1);
        }
    }

    /**
     * Decodes the current state with Hamming Codes ([7, 4] format specifically)
     * Learn more about them at https://en.wikipedia.org/wiki/Hamming_code
     */
    public void decodeHamming() {
        byte[] decoded = new byte[currentState.length / 2];
        for (int i = 0; i < currentState.length; i += 2) {
            int[] bits = new int[8];
            for (int j = 0; j < 2; j++) {
                bits[j * 4] = getBit(currentState[i + j], 2);
                bits[j * 4 + 1] = getBit(currentState[i + j], 4);
                bits[j * 4 + 2] = getBit(currentState[i + j], 5);
                bits[j * 4 + 3] = getBit(currentState[i + j], 6);
            }
            decoded[i / 2] = createByte(bits);
        }
        currentState = decoded;
    }

    /**
     * Encodes the current state with Double-Bit Format
     * More Information in the "DoubleBitEncodingGraphic.png" stored in this repo
     */
    public void encodeDoubleBits() {
        byte[] encodedBytes = new byte[(int) Math.ceil(currentState.length * 8 / 3f)];
        for (int i = 0; i < encodedBytes.length; i++) {
            int parity = 0;
            for (int j = 0; j < 3; j++) {
                int idxBitOverall = i * 3 + j;
                int idxCurBit = idxBitOverall % 8;
                int idxOgByte = idxBitOverall / 8;
                int curBit = currentState.length <= idxOgByte ?
                        0 : getBit(currentState[idxOgByte], idxCurBit);
                parity ^= curBit;
                encodedBytes[i] = setBit(encodedBytes[i], curBit, j * 2);
                encodedBytes[i] = setBit(encodedBytes[i], curBit, j * 2 + 1);
            }
            encodedBytes[i] = setBit(encodedBytes[i], parity, MAX_BIT_INDEX);
            encodedBytes[i] = setBit(encodedBytes[i], parity, MAX_BIT_INDEX - 1);
        }
        currentState = encodedBytes;
    }

    /**
     * Error Corrects the current state with the Double-Bit Format in mind
     * More Information in the "DoubleBitEncodingGraphic.png" stored in this repo
     */
    public void errorCorrectDoubleBits() {
        for(int i = 0; i < currentState.length; i++) {
            int errorIndex = -1;
            int correctBit = 0;
            for (int j = 0; j < 6; j += 2) {
                if ((getBit(currentState[i], j) ^
                        getBit(currentState[i], j + 1)) == 1) {
                    errorIndex = j / 2;
                } else {
                    correctBit += getBit(currentState[i], j);
                }
            }
            if (errorIndex != -1) {
                int parityBit = getBit(currentState[i], MAX_BIT_INDEX);
                correctBit = (correctBit % 2) ^ parityBit;
                currentState[i] = setBit(currentState[i], correctBit,
                        errorIndex * 2);
                currentState[i] = setBit(currentState[i], correctBit,
                        (errorIndex * 2 + 1));
            } else {
                int parityBit = correctBit % 2;
                currentState[i] = setBit(currentState[i], parityBit,
                        MAX_BIT_INDEX - 1);
                currentState[i] = setBit(currentState[i], parityBit, MAX_BIT_INDEX);
            }
        }
    }

    /**
     * Decodes the current state with Double-Bit Format
     * More Information in the "DoubleBitEncodingGraphic.png" stored in this repo
     */
    public void decodeDoubleBits() {
        byte[] decoded = new byte[(int) (currentState.length / 8f * 3)];
        int currentByte = 0;
        int[] curBits = new int[8];
        int curBitIdx = 0;
        for (byte b : currentState) {
            for (int j = 0; j < 6; j += 2) {
                curBits[curBitIdx++] = getBit(b, j);
                if (curBitIdx == 8) {
                    decoded[currentByte++] = createByte(curBits);
                    curBitIdx = 0;
                }
            }
        }
        currentState = decoded;
    }

    /**
     * Simulates errors within the current state of bytes.
     * For every byte, a single bit will be randomly flipped.
     */
    public void simulateErrors() {
        for (int i = 0; i < currentState.length; i++) {
            currentState[i] = flipBit(currentState[i], (int) (Math.random() * BITS_IN_BYTE));
        }
        try (OutputStream outputStream = new FileOutputStream("./received.txt")) {
            outputStream.write(currentState);
        } catch (IOException exception) {
            System.out.println("File could not be written to :(");
        }
    }

    /**
     * Retrieves the string representation of the message
     * @return The current states bytes directly translated into a string of characters
     */
    public String toString() {
        return new String(currentState);
    }

    /**
     * Retrieves the binary representation of the message (space split every 8 bits)
     * @return The current states bytes directly translated into a string of bits
     */
    public String getBinaryRepresentation() {
        String[] result = new String[currentState.length];
        for (int i = 0; i < currentState.length; i++) {
            result[i] = String.
                    format("%8s", Integer.toBinaryString(currentState[i] & 0xFF))
                    .replace(' ', '0');
        }
        return String.join(" ", result);
    }

    /**
     * Retrieves the Hexadecimal representation of the message
     * @return The current states bytes directly translated into a string of Hexadecimals
     */
    public String getHexRepresentation() {
        String[] result = new String[currentState.length];
        for (int i = 0; i < currentState.length; i++) {
            result[i] = String.format("%02x", currentState[i]).toUpperCase();
        }
        return String.join(" ", result);
    }

    /**
     * Retrieves the bit at a certain index of a given byte. (Index starts from 0
     * and counts from left to right)
     * @param bits - The byte that has the desired information
     * @param index - The index at which the desired bit is at in the given byte
     * @return The bit (0 or 1) at the given index in the byte
     */
    public static int getBit(byte bits, int index) {
        return bits >> (7 - index) & 1;
    }

    /**
     * Sets the bit at a certain index of a given byte. (Index starts from 0
     * and counts from left to right)
     * @param bits - The byte that has the desired information
     * @param state - The bit value desired to be set
     * @param index - The index at which the given bit value will be replacing
     * @return The modified byte
     */
    public static byte setBit(byte bits, int state, int index) {
        return (byte) (state == 1 ? bits | (1 << (MAX_BIT_INDEX - index)) :
                bits & ~(1 << (MAX_BIT_INDEX - index)));
    }

    /**
     * Creates a byte from the given bits
     * @param states - An array of 8 integers that represents the bits in a byte
     * @return A byte that represents the bits given
     */
    public static byte createByte(int[] states) {
        byte result = 0;
        for (int i = 0; i < 8; i++) {
            result = (byte) (states[MAX_BIT_INDEX - i] == 1 ?
                    result | (1 << i) : result & ~(1 << i));
        }
        return result;
    }

    /**
     * Flips the bit in a given byte at a desired index
     * @param bits - The byte that will be changed
     * @param index - The index at which the desired bit will be flipped
     * @return The modified byte
     */
    public static byte flipBit(byte bits, int index) {
        return (byte) (bits ^ (1 << (MAX_BIT_INDEX - index)));
    }
}
