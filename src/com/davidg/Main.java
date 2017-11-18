package com.davidg;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// To run:
// java -jar out\artifacts\PostfixResolver_jar\PostfixResolver.jar < data/postfix.csv > data/output.csv
public class Main {
    private static String ERR = "#ERR";
    private static int counter = 0;
    private static ArrayList<List<String>> sheet = new ArrayList<>();

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    private static boolean isCellReference(String str) {
        return str.matches("[a-z]+[0-9]+");
    }

    private static Map<String, Integer> refToXY(String ref) {
        Map<String, Integer> result = new HashMap<>();
        Pattern pattern = Pattern.compile("([a-z]+)(\\d+)");
        Matcher matcher = pattern.matcher(ref);
        matcher.find();
        String letters = matcher.group(1);
        int x = 0;

        // loop through characters (e.g. AAZ) backwards to get total x value
        for (int i = letters.length() - 1; i >= 0; i--) {
            // based on ASCII codes
            int letterValue = letters.codePointAt(i) - 96;
            x += letterValue * Math.pow(26, i);
        }

        int y = Integer.parseInt(matcher.group(2));

        result.put("x", x - 1);
        result.put("y", y - 1);

        return result;
    }

    private static boolean isOperator(String str) {
        // is one of + - / *
        return str.matches("(\\+|\\/|-|\\*)");
    }

    private static String operate(String operand1, String operand2, String operator) {
        double operand1Double = Double.parseDouble(operand1);
        double operand2Double = Double.parseDouble(operand2);
        double result;

        switch (operator) {
            case "+": result = operand1Double + operand2Double;
                break;
            case "-": result = operand1Double - operand2Double;
                break;
            case "*": result = operand1Double * operand2Double;
                break;
            case "/": result = operand1Double / operand2Double;
                break;
            default: return ERR;
        }

        // we don't want to show decimal places if not required, so...
        if (result == (int)result) {
            return String.format("%d", (int)result);
        } else {
            return String.format("%s", result);
        }
    }

    private static String getCellValueAtRef(String ref) {
        int MAX_OPS = 1000;
        counter++;
        if (counter > MAX_OPS) {
            return ERR;
        }

        Map<String, Integer> pos = refToXY(ref);
        int x = pos.get("x");
        int y = pos.get("y");

        try {
            // the sheet is rows, then cols, so y then x
            String parsedCellContents = parseCell(sheet.get(y).get(x));

            // update the cell as we go if required
            sheet.get(y).set(x, parsedCellContents);

            return parsedCellContents;
        } catch (IndexOutOfBoundsException e) {
            return ERR;
        }
    }

    private static String parsePostfix(String postfixString) {
        List<String> postfixStack = new ArrayList<>(Arrays.asList(postfixString.toLowerCase().split(" ")));

        for (int i = 0; i < postfixStack.size(); i++) {
            String part = postfixStack.get(i).trim();
            if (isCellReference(part)) {
                String cellValue = getCellValueAtRef(part);
                if (Objects.equals(cellValue, ERR)) return ERR;

                postfixStack.set(i, cellValue);
            } else if (isOperator(part)) {
                if (i < 2)  return ERR;

                String result = operate(postfixStack.get(i - 2), postfixStack.get(i - 1), part);

                i -= 2;
                postfixStack.remove(i);
                postfixStack.remove(i);
                postfixStack.set(i, result);
            } else if (!isNumeric(part)) {
                return ERR;
            }
        }

        if (postfixStack.size() > 1) {
            return ERR;
        } else {
            return postfixStack.get(0);
        }
    }

    private static String parseCell(String cell) {
        String trimmedCell = cell.trim();

        if (isNumeric(trimmedCell)) {
            return trimmedCell;
        } else {
            return parsePostfix(trimmedCell);
        }
    }

    private static void parseSheet() {
        for (List<String> row : sheet) {
            for (int c = 0; c < row.size(); c++) {
                counter = 0;
                String parsedCell = parseCell(row.get(c));

                row.set(c, parsedCell);
            }

            System.out.println(String.join(", ", row));
        }
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextLine()) {
            String row = scanner.nextLine();

            List<String> cells = new ArrayList<>(Arrays.asList(row.split(",")));
            sheet.add(cells);
        }

        parseSheet();

//        System.out.println("HELLO: " + args[0]);
//        FileReader fileReader = new FileReader(args[0]);
//        BufferedReader bufferedReader = new BufferedReader(fileReader);
//        String row;
//
//        while((row = bufferedReader.readLine()) != null) {
//            List<String> cells = new ArrayList<>(Arrays.asList(row.split(",")));
//            sheet.add(cells);
//        }
//
//        parseSheet();
//
//        fileReader.close();
    }
}
