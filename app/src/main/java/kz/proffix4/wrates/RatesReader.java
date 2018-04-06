package kz.proffix4.wrates;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// СОЗДАТЕЛЬ КОТИРОВОК ВАЛЮТ
public class RatesReader {

    private static final String BASE_URL = "http://qazkom.kz"; // Адрес с котировками

    // Парсинг котировок из формата html web-страницы банка, при ошибке доступа возвращаем null
    public static String getRatesData() {
        StringBuilder data = new StringBuilder();
        try {
            Document doc = Jsoup.connect(BASE_URL).get(); // Создание документа JSOUP из html
            //data.append(doc.title().split("—")[0].trim()).append(":"); // Считываем заголовок страницы
            data.append("Курсы валюты:\n"); // Считываем заголовок страницы
            data.append(String.format("%12s %12s %12s", "валюта", "покупка", "продажа"));
            Elements e = doc.select("div.exchange-well"); // Ищем в документе "<div class="exchange-well"> с данными о валютах
            Elements tables = e.select("table"); // Ищем таблицы с котировками
            Element table = tables.get(0); // Берем 1 таблицу для физических лиц
            // Цикл по строкам таблицы
            int i=0;
            for (Element row : table.select("tr")) { // Пропускаем 1 строку с заголовком (eq - конкретная , lt - ниже, gt - выше)
                // Цикл по столбцам таблицы
                for (Element col : row.select("td")) { //
                    data.append(String.format("%12s", col.text())); // Считываем данные с ячейки таблицы
                }
                if (i++>2) break; else
                    data.append("\n"); // Добавляем переход на следующую строку;
            }

        } catch (Exception ignored) {
            return null; // При ошибке доступа возвращаем null
        }
        return data.toString(); // Возвращаем результат
    }

}