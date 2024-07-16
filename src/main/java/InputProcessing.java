import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class InputProcessing {

    public List<String> processInputData(String message_text) {
        List<String> generatedUrls = new ArrayList<>();
        String formattedMessage = message_text.replaceAll(" ", "-").replaceAll("--+", "-");

        // Разделение строки на массив строк по указанным окончаниям
        String[] lines = formattedMessage.split("\n");
        //String[] lines = formattedMessage.split("(?i)(?<=В|в|V|v|VDC|vdc|kVDC|KVDC|kvDC|kvdc|kvdC|KVdc|Kvdc|kVdc|вольт|ВОЛЬТ|ВТ|вт|киловольт|КИЛОВОЛЬТ|КВТ|квт)");

        // Обработка каждой строки
        for (String line : lines) {
            String capacitance = extractCapacitance(line);
            line = line.replace(capacitance, "");
            String caseCode = extractCaseCode(line);
            line = line.replace(caseCode, "");
            String tolerance = extractTolerance(line);
            line = line.replace(tolerance, "");
            String voltage = extractVoltage(line);
            line = line.replace(voltage, "");
            String dielectric = extractDielectric(line);
            line = line.replace(dielectric, "");
            String caseInfo = extractCase(line);
            line = line.replace(caseInfo, "");

            try {
                String url = generateUrl(capacitance, caseCode, tolerance, voltage, dielectric, caseInfo);
                generatedUrls.add(url);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }


            System.out.println("Capacitance: " + capacitance);
            System.out.println("Case Code: " + caseCode);
            System.out.println("Tolerance: " + tolerance);
            System.out.println("Voltage: " + voltage);
            System.out.println("Dielectric: " + dielectric);
            System.out.println("Case: " + caseInfo);
            System.out.println();
        }
        return generatedUrls;
    }

    public static String generateUrl(String capacitance, String caseCode,
                                     String tolerance, String voltage,
                                     String dielectric, String caseInfo) throws UnsupportedEncodingException {

        String capacitanceNumber = splitString(capacitance)[0];
        String capacitanceText = splitString(capacitance)[1];
        caseCode = caseCode.replaceAll("\\D+", "");
        String toleranceNumber = splitString(tolerance)[0];
        String toleranceText = splitString(tolerance)[1];
        voltage = voltage.replaceAll("\\D+", "");
        caseInfo = caseInfo.toUpperCase();

        String encodedCapacitance = "capacitance=" + URLEncoder.encode(capacitanceNumber, "UTF-8") + "%20" + URLEncoder.encode(capacitanceText.replace("-", "").replace(" ", ""), "UTF-8");
        String encodedCaseCode = "case%20code%20-%20in=" + URLEncoder.encode(caseCode, "UTF-8");
        String encodedTolerance = "tolerance=" + URLEncoder.encode(toleranceNumber, "UTF-8") + "%20" + URLEncoder.encode(toleranceText.replace("-", "").replace(" ", ""), "UTF-8");
        String encodedVoltage = "voltage%20rating%20dc=" + URLEncoder.encode(voltage, "UTF-8") + "%20VDC";
        String encodedDielectric = "dielectric=" + URLEncoder.encode(dielectric, "UTF-8");
        String encodedCaseInfo = "mfr%20case%20code=" + URLEncoder.encode(caseInfo, "UTF-8") + "%20Case";

        // Формируем URL с уже закодированными параметрами

        StringBuilder urlBuilder;

        if (caseInfo != null && !caseInfo.isEmpty()) {
            urlBuilder = new StringBuilder("https://eu.mouser.com/c/passive-components/capacitors/tantalum-capacitors/?");
        } else {
            urlBuilder = new StringBuilder("https://eu.mouser.com/c/passive-components/?");
        }


        if (!capacitance.isEmpty()) {
            urlBuilder.append(encodedCapacitance).append("&");
        }

        if (!caseCode.isEmpty()) {
            urlBuilder.append(encodedCaseCode).append("&");
        }

        if (!tolerance.isEmpty()) {
            urlBuilder.append(encodedTolerance).append("&");
        }

        if (!voltage.isEmpty()) {
            urlBuilder.append(encodedVoltage).append("&");
        }

        if (!dielectric.isEmpty()) {
            urlBuilder.append(encodedDielectric).append("&");
        }

        if (!caseInfo.isEmpty()) {
            urlBuilder.append(encodedCaseInfo).append("&");
        }

        // Удаляем последний символ '&' если он есть
        if (urlBuilder.charAt(urlBuilder.length() - 1) == '&') {
            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
        }

        // Добавляем параметр instock
        urlBuilder.append("&instock=y");

        return urlBuilder.toString();

    }

    public static String extractCapacitance(String line) {
        Pattern pattern = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)(пФ|ПФ|Пф|мкФ|МКФ|мкф|Мкф|мКФ|нФ|Нф|НФ|uF|Uf|UF|pF|Pf|PF|nF|Nf|NF|" +
                "-пФ|-ПФ|-Пф|-мкФ|-МКФ|-мкф|-Мкф|-мКФ|-нФ|-Нф|-НФ|-uF|-Uf|-UF|-pF|-Pf|-PF|-nF|-Nf|-NF)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String value = matcher.group(1).replace(',', '.');
            String unit = matcher.group(2).toLowerCase().replaceAll("-", "");
            unit = switch (unit) {
                case "пф", "pf" -> "pF";
                case "мкф", "uf" -> "uF";
                case "нф", "nf" -> "nF";
                default -> unit;
            };

            return (value + unit).replace("-", "").replace(" ", "");
        }
        return "";
    }

    public static String extractCaseCode(String line) {
        Pattern pattern = Pattern.compile("(\\d{4})");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1).replace("-", "").replace(" ", "");
        }
        return "";
    }

    public static String extractTolerance(String line) {
        //Pattern pattern = Pattern.compile("±-?(\\d+\\.?\\d*)\\s*(-?%|-?пФ|-?пф|-?Пф|-?ПФ|-?pF|-?Pf|-?PF|-?pf)");
        Pattern pattern = Pattern.compile("(\\d+\\.?\\d*)\\s*(-?%|±-?)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String value = matcher.group(1);
            String unit = matcher.group(2).replaceAll("-", "");
            switch (unit) {
                case "%":
                    return (value + "%").replace("-", "").replace(" ", "");
                case "пФ":
                case "pF":
                    return (value + "pF").replace("-", "").replace(" ", "");
            }
        }
        return "";
    }

    public static String extractVoltage(String line) {
        Pattern pattern = Pattern.compile("(\\d+\\.?\\d*)(B|b|В|в|V|v|VDC|vdc|kVDC|KVDC|kvDC|kvdc|kvdC|KVdc|Kvdc|kVdc|вольт|ВОЛЬТ|ВТ|вт|киловольт|КИЛОВОЛЬТ|КВТ|квт)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String value = matcher.group(1);
            String unit = matcher.group(2).toUpperCase();
            unit = "VDC";
            return (value + unit).replace("-", "").replace(" ", "");
        }
        return "";
    }

    public static String extractDielectric(String line) {
        if (line.toUpperCase().contains("class") || line.toLowerCase().contains("класс")) {
            return "Class 2";
        } else if (line.toUpperCase().contains("C0G") && line.toUpperCase().contains("NP0") ||
                line.toUpperCase().contains("COG") && line.toUpperCase().contains("NPO") ||
                line.toUpperCase().contains("C0G") && line.toUpperCase().contains("NPO") ||
                line.toUpperCase().contains("COG") && line.toUpperCase().contains("NP0")) {
            return "C0G (NP0)";
        } else if (line.toUpperCase().contains("PLZT")) {
            return "PLZT Ceramic";
        } else if (line.toUpperCase().contains("HIGH")) {
            return "High Q";
        }
        Pattern pattern = Pattern.compile("(?i)(AH|B|BD|BE|BG|BH|BJ|BL|BN|BP|BQ|BR|BU|BV|BX|C|C[0O]G|C[0O]H|C[0O]J|C[0O]K|CA|CCG|CD|CF|CG|CGJ|CH|CHA|CK|CX|CX7R" +
                "|F|G|GA|H|High|Q|J|JB|K|L|M|N|N1500|N2000|N2200|N2500|N2800|N4700|NP|NP[0O]|NR|NS|NU|P3K|P90|R16|R2D|R3L|R42|R7|R85|S3N|SL|SL[0O]|T3M" +
                "|U2J|U2K|UNJ|UX|V|VHT|X0U|X5E|X5F|X5R|X5S|X5U|X6S|X6T|X76|X7R|X7S|X7T|X7U|X8G|X8L|X8M|X8N|X8R|X9M|X9Q|Y5E|Y5P|Y5R|Y5S|Y5T|Y5U|Y5V|Y6P|Z5F|Z5P|Z5T|Z5U|Z5V|ZLM)" +
                "(?!\\w)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find() && extractCase(line).isEmpty()) {
            return matcher.group(1).toUpperCase().replace('O', '0').replace("-", "").replace(" ", "");
        }
        return "";
    }

    public static String extractCase(String line) {
        // Удаляем все двойные кавычки из строки
        line = line.replace("\"", "");

        // Регулярное выражение для поиска ключевых слов с учетом регистра и извлечения 1-3 символов после пробела или черточки
        Pattern pattern = Pattern.compile("(?i)\\b(корпус|case)\\b[ -]?([A-Za-z0-9]{1,3})");
        Matcher matcher = pattern.matcher(line);

        // Если найдено совпадение, возвращаем найденную группу символов в верхнем регистре
        if (matcher.find()) {
            return matcher.group(2).toUpperCase().replace("-", "").replace(" ", "");
        }
        return "";
    }

    public static String[] splitString(String s) {
        // Find the index of the first letter or '%' character
        OptionalInt firstLetterOrPercentIndex = IntStream.range(0, s.length())
                .filter(i -> Character.isLetter(s.charAt(i)) || s.charAt(i) == '%')
                .findFirst();

        // Default if there are only numbers
        String numbers = s;
        String lettersOrPercent = "";

        // If there are letters or '%', split the string at the first occurrence
        if (firstLetterOrPercentIndex.isPresent()) {
            numbers = s.substring(0, firstLetterOrPercentIndex.getAsInt());
            lettersOrPercent = s.substring(firstLetterOrPercentIndex.getAsInt());
        }

        return new String[]{numbers, lettersOrPercent};
    }

}
