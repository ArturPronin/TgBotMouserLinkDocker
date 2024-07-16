import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class ConnectionURL {

    public static Document parsePage(String url) {
        Document doc = null;
        int attempts = 0;
        int maxAttempts = 5;

        while (attempts < maxAttempts) {
            try {
                doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                        .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                        .header("Accept-Encoding", "gzip, deflate, br, zstd")
                        .header("Cache-Control", "no-cache")
                        .header("Cookie", "ASP.NET_SessionId=kohdnkz0dclft2amk42owxum; CARTCOOKIEUUID=a4796e77-f55a-4649-9f36-3ac215fad641; __RequestVerificationToken=FBPWNIyUAvqFzWuBNlXsyb9YOTteCror0XC3gWOMxys3CMerhYemrOdoMDOLViuQglAuFb0JeRcNJOTrzmUCqxoH0B01; akacd_Default_PR=3894455606~rv=7~id=3a5bf3eb64ab6c043ea8689e72e432dd; OptanonAlertBoxClosed=2024-05-29T17:13:32.393Z; __neoui=bba563d1-348c-47e1-98c5-39b82db21766; LPVID=U1MzFkN2VkODFjYjk2MzU4; PIM-SESSION-ID=d7AAOwuO683QmUcU; enableFBPixel=true; LPSID-12757882=g95vs8pkTl-N0LKJJye5UQ; bm_sz=EFA0989D8B0723DD36A91863C8BE4B3A~YAAQrOxlX2Mmh7uPAQAAUj6FxhdSixbCwqzdeQ+IJ4wrETYWOVd0Q5k1PFoxtIkOyO/mKyc8llrLRy4kDT4F2l2LDZGlCY5RNbqwCYjgMA4OtiTVnRLtXhTGlMHsg1JCrRI11FKSQgRVINYbmjOFpq86NuozMHpa42fLBqhU3iiwfUgwThf5OayPdhZYRPIkXTEEwFYQGX0Xf91kr8f+eIq6BIB0kXYWvlPzLIzI2cKX3ydw3FdlUlPCjOK4nTbQ7dwkybQsH4B7hFIjcB360gCw6RX4cvNZKu8b0HRk6ahAA3ARJNDpChxxEYvixN1A7ugS3pcIB3a6Duzqbo1sjH1VLpC30GRScRn7ANx9FsuCzE6JqcQ1pVmh/FavIsI1sqETXLWckBV4R7xKBcdQy6eFpNm75BnzVY4efHhPVZ/QmgVkJw8XQJGYWv/B+InEyjg+ZkeOUll+WQQRJsfou6Jpcl8kFLI61eXIE6i5N17VLBZByPOCeTPetoO697w64Nfaz6T6r9XonOVdtxzXo7OipfZHlT5BXWp7QwV5pSniySGUEj1GSnQ2/Sxn8QUJC6pDmWQU/W0=~3360066~4473158; preferences=FF=&ps=eu&pl=en-GB&pc_eu=EUR; OptanonConsent=isGpcEnabled=0&datestamp=Thu+May+30+2024+01%3A42%3A55+GMT%2B0300+(%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C+%D1%81%D1%82%D0%B0%D0%BD%D0%B4%D0%B0%D1%80%D1%82%D0%BD%D0%BE%D0%B5+%D0%B2%D1%80%D0%B5%D0%BC%D1%8F)&version=202403.1.0&browserGpcFlag=0&isIABGlobal=false&hosts=&consentId=ed0e55d1-76dc-488f-9ae7-4bc228034293&interactionCount=1&isAnonUser=1&landingPath=NotLandingPage&groups=C0001%3A1%2CC0002%3A1%2CC0004%3A1&geolocation=FR%3B&AwaitingReconsent=false; datadome=Cobaz4qJHMUGHEIBOXXK2nmXk0MY347G1bGGLe5HBPaI6OH9kotF3k0b7PSxpKB4QDMXNee40U18CIzryN~7D9X7jKUnjTz23dTcA_fILXEVvrNzkrOpSgMwD6AUZmZX; _abck=28C3CF443C7BE618BB7008665BDBAB2A~-1~YAAQoD4SAoAjrK2PAQAAGtaSxgsRaxeov7R6zJhWP2A/PtjOUuaXVtgptPJ4brNL7sFfiUVjAAtf9Piss9UUmBWENFNz/9CEUvQxKA/9ldH4e596+bG6cV3akUWfRjYaNw3GTFpSX90kpR2+vtZDbgUebXV/SOkN94Aus/ZcGAiMMjDig0tuwmaYIxB5DDONakCm1QRDY8lCL2PrwaXGzZ49+r1dagV3jUhlUnaU8Rpdf8ML9L6nSU0zs69g/F2+6Sb89OHg+XWP5LeWaJj8TciqdBr3fE8W2rOVL70FnW6es06OSIN9aDQ3e6drK2vA1w5/JTebbzpEhjLsS+CDviH6dxh55QRYJ77fxLRrVyIPFcm9gE0jjYK3ww5FGH9nRyJrKqMiNaXrL46oyoJWX2uNusyJuw==~-1~-1~-1")
                        .header("Referer", "https://eu.mouser.com/")
                        .header("Pragma", "no-cache")
                        .header("Priority", "u=0, i")
                        .header("Sec-Ch-Device-Memory", "8")
                        .header("Sec-Ch-Ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"")
                        .header("Sec-Ch-Ua-Arch", "\"x86\"")
                        .header("Sec-Ch-Ua-Full-Version-List", "\"Google Chrome\";v=\"125.0.6422.78\", \"Chromium\";v=\"125.0.6422.78\", \"Not.A/Brand\";v=\"24.0.0.0\"")
                        .header("Sec-Ch-Ua-Mobile", "?0")
                        .header("Sec-Ch-Ua-Model", "\"\"")
                        .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                        .header("Sec-Fetch-Dest", "document")
                        .header("Sec-Fetch-Mode", "navigate")
                        .header("Sec-Fetch-Site", "none")
                        .header("Sec-Fetch-User", "?1")
                        .header("Upgrade-Insecure-Requests", "1")
                        .header("Connection", "keep-alive")
                        .get();

                return doc; //!doc.body().text().contains(noResultsText);
            } catch (IOException e) {
                attempts++;
                if (attempts >= maxAttempts) {
                    throw new RuntimeException("<b>Не удалось подключится по URL спустя " + maxAttempts + " попыток.</b>\nПроблема на стороне бота (интернет или VPN). Попробуйте изменить запрос или обратиться позднее.");
                }
            }
        }
        return doc;
    }
}
