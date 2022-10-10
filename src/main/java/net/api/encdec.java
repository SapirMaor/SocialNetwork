package net.api;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class encdec implements MessageEncoderDecoder<String> {
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;

    @Override
    public String decodeNextByte(byte nextByte) {
        if (nextByte == ';') {
            return popString();
        }

        pushByte(nextByte);
        return null; //not a line yet
    }

    @Override
    public byte[] encode(String message) {
        String[] splitMsg = message.split(" ", -2);
        Short op = Short.parseShort(splitMsg[0]);
        byte[] opcodeByte = shortToBytes(op);
        Short msg = Short.parseShort(splitMsg[1]);
        byte[] msgType = shortToBytes(msg);
        byte[] zero = "0".getBytes(StandardCharsets.UTF_8);
        byte[] username, one, content;
        byte[] result = {};

        switch (op) {
            case 9: //Notification Message
                switch (msg){
                    case 0: //PM message
                        username = splitMsg[2].getBytes(StandardCharsets.UTF_8);
                        content = message.substring(5 + username.length).getBytes(StandardCharsets.UTF_8);
                        //content = splitMsg[3].getBytes(StandardCharsets.UTF_8);
                        result = join(new byte[][]{opcodeByte, zero, username, "\0".getBytes(StandardCharsets.UTF_8), content, "\0".getBytes(StandardCharsets.UTF_8)}, 5 + username.length + content.length);
                        break;
                    case 1: //Post message
                        username = splitMsg[2].getBytes(StandardCharsets.UTF_8);
                        one = "1".getBytes(StandardCharsets.UTF_8);
                        content = message.substring(5 + username.length).getBytes(StandardCharsets.UTF_8);
                        //content = splitMsg[3].getBytes(StandardCharsets.UTF_8);
                        result = join(new byte[][]{opcodeByte, one, username, "\0".getBytes(StandardCharsets.UTF_8), content, "\0".getBytes(StandardCharsets.UTF_8)}, 5 + username.length + content.length);
                        break;
                }
                break;
            case 10: //ACK Message
                switch (msg){
                    case 1: //register
                    case 2: //login
                    case 3: //logout
                    case 5: //post
                    case 6: //PM
                    case 12: //BLOCK
                        result = join(new byte[][]{opcodeByte, msgType}, 4);
                        break;
                    case 4: //follow/unfollow
                        username = splitMsg[2].getBytes(StandardCharsets.UTF_8);
                        result = join(new byte[][]{opcodeByte, msgType, username, "\0".getBytes(StandardCharsets.UTF_8)}, 5 + username.length);
                        break;
                    case 7: //logstat
                    case 8: //stat
                        String[] users = message.split("\0", -2);
                        for(String user : users){
                            String[] userB = user.split(" ", -2);
                            byte[] opCode = shortToBytes(Short.parseShort(userB[0]));
                            byte[] msType = shortToBytes(Short.parseShort(userB[1]));
                            byte[] age = shortToBytes(Short.parseShort(userB[2]));
                            byte[] numPosts = shortToBytes(Short.parseShort(userB[3]));
                            byte[] numFollowers = shortToBytes(Short.parseShort(userB[4]));
                            byte[] numFollowing = shortToBytes(Short.parseShort(userB[5]));
                            result = join(new byte[][]{result, opCode, msType, age, numPosts, numFollowers, numFollowing, "\0".getBytes(StandardCharsets.UTF_8)}, 13 + result.length);
                        }
                        break;
                }
                break;
            case 11: //Error Message
                switch (msg){
                    case 1: //register
                    case 2: //login
                    case 3: //logout
                    case 4: //follow/unfollow
                    case 5: //post
                    case 6: //pm
                    case 7: //logstat
                    case 8: //stat
                    case 12: //block
                        result = join(new byte[][]{opcodeByte, msgType}, 4);
                        break;
                }
                break;
        }
        result = join(new byte[][]{result, ";".getBytes()}, result.length +1);
        return result;
    }

    private byte[] join(byte[][] b, int length){
        byte[] allBytes = new byte[length];
        int startIndex = 0;
        for(byte[] currB : b){
            int currLength = currB.length;
            System.arraycopy(currB, 0, allBytes, startIndex, currLength);
            startIndex = startIndex + currLength;
        }
        return allBytes;
    }


    public byte[] shortToBytes(short num)
    {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte)((num >> 8) & 0xFF);
        bytesArr[1] = (byte)(num & 0xFF);
        return bytesArr;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len++] = nextByte;
    }
    private String popString() {
        //get op and work accordingly
        short op = bytesToShort(Arrays.copyOfRange(bytes,0,2));
        String result ="";
        switch (op) {
            case 1: //Register
            case 5: //Post message
            case 6: //PM message
            case 8: //STAT message
            case 12: //Block Message
                result = String.valueOf(op) + splitByZero(Arrays.copyOfRange(bytes, 2, len));
                    break;
            case 2:
                result = String.valueOf(op) + splitByZero(Arrays.copyOfRange(bytes, 2, len - 1)) + " " + bytes[len- 1]; //Login
            break;
            case 3: // Logout
            case 7: // Logstat
                result = String.valueOf(op);
                break;
            case 4: //Follow/Unfollow
                result = String.valueOf(op) + " " + bytes[2] + " " + new String(bytes, 3, len-4   );
            break;
        }
        len = 0;
        return result;
    }
    public short bytesToShort(byte[] byteArr) {
        short result = (short)((byteArr[0] & 0xff) << 8);
        result += (short)(byteArr[1] & 0xff);
        return result;
    }
    private String splitByZero(byte[] byteArr){
        String str = "";
        //str += bytesToShort(Arrays.copyOfRange(byteArr, 0, 2));
        int index = 0;
        for (int i = 0; i < byteArr.length; i++) {
            if(byteArr[i]==0){
                str+= " " + new String(byteArr,index, i - index);
                index= i+1;
            }
        }
        return str;
    }
}
