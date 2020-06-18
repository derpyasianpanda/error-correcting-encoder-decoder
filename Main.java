import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        boolean formatting = true;
        String format = "";
        while (formatting) {
            System.out.print("Choose a proper Encoding/Decoding format (H)amming, (D)ouble-Bit Encoding: ");
            format = new Scanner(System.in).nextLine().trim().toLowerCase();
            formatting = !("h".equals(format) || "d".equals(format));
        }
        System.out.println();
        EncoderDecoder encoderDecoder;
        boolean running = true;

        while (running) {
            System.out.println("Program set to " +
                    ("h".equals(format) ? "Hamming Code" : "Double-Bit") + " Encoding/Decoding");
            System.out.print("Write a mode (Encode, Send, Decode): ");
            String mode = new Scanner(System.in).next().toLowerCase();
            switch (mode) {
                case "e":
                case "encode":
                    encoderDecoder = new EncoderDecoder("./send.txt", format);
                    System.out.println("\nsend.txt:");
                    System.out.println("Text view: " + encoderDecoder);
                    System.out.println("Hexadecimal view: " + encoderDecoder.getHexRepresentation());
                    System.out.println("Binary view: " + encoderDecoder.getBinaryRepresentation());

                    encoderDecoder.encode();
                    System.out.println("\nencoded.txt:");
                    System.out.println("Binary view: " + encoderDecoder.getBinaryRepresentation());
                    System.out.println("Hexadecimal view: " + encoderDecoder.getHexRepresentation());
                    break;
                case "s":
                case "send":
                    encoderDecoder = new EncoderDecoder("./encoded.txt", format);
                    System.out.println("\nencoded.txt:");
                    System.out.println("Hexadecimal view: " + encoderDecoder.getHexRepresentation());
                    System.out.println("Binary view: " + encoderDecoder.getBinaryRepresentation());

                    encoderDecoder.simulateErrors();
                    System.out.println("\nreceived.txt:");
                    System.out.println("Binary view: " + encoderDecoder.getBinaryRepresentation());
                    System.out.println("Hexadecimal view: " + encoderDecoder.getHexRepresentation());
                    break;
                case "d":
                case "decode":
                    encoderDecoder = new EncoderDecoder("./received.txt", format);
                    System.out.println("\nreceived.txt:");
                    System.out.println("Hexadecimal view: " + encoderDecoder.getHexRepresentation());
                    System.out.println("Binary view: " + encoderDecoder.getBinaryRepresentation());

                    encoderDecoder.errorCorrect();
                    System.out.println("\ndecoded.txt:");
                    System.out.println("correct: " + encoderDecoder.getBinaryRepresentation());

                    encoderDecoder.decode();
                    System.out.println("decoded: " + encoderDecoder.getBinaryRepresentation());
                    System.out.println("Hexadecimal view: " + encoderDecoder.getHexRepresentation());
                    System.out.println("Text view: " + encoderDecoder);
                    break;
                case "default":
                    System.out.println("\nThis option doesn't exist");
                    break;
            }
            System.out.println();
            System.out.print("Would you like to end the program? (y/N): ");
            running = !(new Scanner(System.in).nextLine().toLowerCase().trim().equals("y"));
            System.out.println();
        }
        System.out.println("Thanks for using this program :D");
    }
}

class EncoderDecoder {
    public static int MAX_BIT_INDEX = 7;
    public static int BITS_IN_BYTE = 8;

    private byte[] currentState;
    private String format;

    public EncoderDecoder(String fileName, String format) {
        try (InputStream inputStream = new FileInputStream(fileName)) {
            currentState = inputStream.readAllBytes();
        } catch (FileNotFoundException exception) {
            System.out.println("File wasn't found :(");
        } catch (IOException exception) {
            System.out.println(exception.getMessage());
        }
        this.format = format;
    }

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

    public void errorCorrect() {
        if ("h".equals(format)) {
            errorCorrectHamming();
        } else {
            errorCorrectDoubleBits();
        }
    }

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

    public void encodeHamming() {
        byte[] encodedBytes = new byte[currentState.length * 2];
        for (int i = 0; i < encodedBytes.length; i++) {
            int[] infoBits = new int[4];
            int idxOgByte = i / 2;
            for (int j = 0; j < 4; j++) {
                int idxBit = (i % 2) * 4 + j;
                infoBits[j] = getBit(currentState[idxOgByte], idxBit);
            }
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

    public void errorCorrectHamming() {
        for (int i = 0; i < currentState.length; i++) {
            currentState[i] = flipBit(currentState[i],
                    ((getBit(currentState[i], 0) + getBit(currentState[i], 2) +
                            getBit(currentState[i], 4) + getBit(currentState[i], 6)) % 2 +
                            (getBit(currentState[i], 1) + getBit(currentState[i], 2) +
                                    getBit(currentState[i], 5) + getBit(currentState[i], 6)) % 2 * 2 +
                            (getBit(currentState[i], 3) + getBit(currentState[i], 4) +
                                    getBit(currentState[i], 5) + getBit(currentState[i], 6)) % 2 * 4) - 1);
        }
    }

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

    public String toString() {
        return new String(currentState);
    }

    public String getBinaryRepresentation() {
        String[] result = new String[currentState.length];
        for (int i = 0; i < currentState.length; i++) {
            result[i] = String.
                    format("%8s", Integer.toBinaryString(currentState[i] & 0xFF))
                    .replace(' ', '0');
        }
        return String.join(" ", result);
    }

    public String getHexRepresentation() {
        String[] result = new String[currentState.length];
        for (int i = 0; i < currentState.length; i++) {
            result[i] = String.format("%02x", currentState[i]).toUpperCase();
        }
        return String.join(" ", result);
    }

    public static int getBit(byte bits, int index) {
        return bits >> (7 - index) & 1;
    }

    public static byte setBit(byte bits, int state, int index) {
        return (byte) (state == 1 ? bits | (1 << (MAX_BIT_INDEX - index)) :
                bits & ~(1 << (MAX_BIT_INDEX - index)));
    }

    public static byte createByte(int[] states) {
        byte result = 0;
        for (int i = 0; i < 8; i++) {
            result = (byte) (states[MAX_BIT_INDEX - i] == 1 ?
                    result | (1 << i) : result & ~(1 << i));
        }
        return result;
    }

    public static byte flipBit(byte bits, int index) {
        return (byte) (bits ^ (1 << (MAX_BIT_INDEX - index)));
    }
}
