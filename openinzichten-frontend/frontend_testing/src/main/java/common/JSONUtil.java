package common;

import com.google.gson.JsonParser;

import java.io.FileReader;

public class JSONUtil {
    public static String getValueFromJsonFile(String filePath, String key) {
        try {
            FileReader fr = new FileReader(filePath);
            JsonParser parser = new JsonParser();
            var jsonObject = parser.parse(fr).getAsJsonObject();
            return jsonObject.get(key).getAsString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
