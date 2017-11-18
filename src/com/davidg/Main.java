package com.davidg;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static String ERR = "#ERR";
    private static int counter = 0;

    // input will be read into this sheet 2D array list
    // Note that strings are passed around at all times except when an operation is being done
    private static ArrayList<List<String>> sheet = new ArrayList<>();

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    private static boolean isCellReference(String str) {
        // check if the string is a valid cell reference, e.g. A2 or BAZ99
        return str.matches("[a-z]+[0-9]+");
    }

    private static Map<String, Integer> refToXY(String ref) {
        Map<String, Integer> result = new HashMap<>();

        // split apart the letters/number parts.
        Pattern pattern = Pattern.compile("([a-z]+)(\\d+)");
        Matcher matcher = pattern.matcher(ref);
        matcher.find();
        String letters = matcher.group(1);

        // loop through letters (e.g. AAZ) backwards to get total x value
        int x = 0;
        for (int i = letters.length() - 1; i >= 0; i--) {
            // based on a = 97 in Unicode/ASCII
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

        // this is a bit repetitive, but something like ScriptEngine seemed overkill
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
            // the sheet is nested by rows, then columns, so we get by y, then x
            String parsedCellContents = parseCell(sheet.get(y).get(x));

            // update the cell as we go
            sheet.get(y).set(x, parsedCellContents);

            return parsedCellContents;
        } catch (IndexOutOfBoundsException e) {
            // if a cell that doesn't exist is referenced (e.g. aaZ1)
            return ERR;
        }
    }

    private static String parsePostfix(String postfixString) {
        // split the contents of the cell by whitespace (this is trimmed later)
        List<String> postfixStack = new ArrayList<>(Arrays.asList(postfixString.toLowerCase().split(" ")));

        // for each item in the stack
        for (int i = 0; i < postfixStack.size(); i++) {
            String part = postfixStack.get(i).trim();

            if (isCellReference(part)) {
                // if it's a cell reference, first go and get the value of that cell
                String cellValue = getCellValueAtRef(part);
                if (Objects.equals(cellValue, ERR)) return ERR;

                // and replace the reference in the stack with the resolved value
                postfixStack.set(i, cellValue);
            } else if (isOperator(part)) {
                if (i < 2) return ERR;

                // get the result of the operation
                String result = operate(postfixStack.get(i - 2), postfixStack.get(i - 1), part);

                // replace the last two numbers and the operator with the result
                i -= 2;
                postfixStack.remove(i);
                postfixStack.remove(i);
                postfixStack.set(i, result);
            } else if (!isNumeric(part)) {
                // Any part of a postfix string that isn't a cell reference, a number, or an operator, is an error
                return ERR;
            }
        }

        // If we wound up with more than one item on the stack, the input was not valid postfix (e.g. "1 2 3")
        if (postfixStack.size() > 1) {
            return ERR;
        } else {
            // else return the only item
            return postfixStack.get(0);
        }
    }

    private static String parseCell(String cell) {
        String trimmedCell = cell.trim();

        if (isNumeric(trimmedCell)) {
            // if a cell contains only a number, return it
            return trimmedCell;
        } else {
            // else is might be postfix notation or a single cell reference
            // these are treated in the same way
            return parsePostfix(trimmedCell);
        }
    }

    private static void parseSheet() {
        // loop through each row and cell, updating the cells in the sheet as we go
        for (List<String> row : sheet) {
            for (int c = 0; c < row.size(); c++) {
                counter = 0;
                String parsedCell = parseCell(row.get(c));

                row.set(c, parsedCell);
            }

            // once each row is done, output it
            System.out.println(String.join(", ", row));
        }
    }

    public static void main(String[] args) throws Exception {
        // here we read stdin into a 2D array "sheet"
        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextLine()) {
            String row = scanner.nextLine();

            List<String> cells = new ArrayList<>(Arrays.asList(row.split(",")));
            sheet.add(cells);
        }

        // then kick off the parsing of that sheet.
        parseSheet();
    }
}
