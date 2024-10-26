package jenkins.callbacktransfer.yzj;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
public class YZJBot {
    private final String name;
    private final String url;

    public YZJBot(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public boolean notifyByParts(String bigContent, int partLength, List<String> limitPhoneNums) {
        List<String> list = splitByLength(bigContent, partLength);
        boolean success = true;
        for (String part : list) {
            if (success) {
                success = notify(part, limitPhoneNums);
            }
        }
        return success;
    }

    private static List<String> splitByLength(String str, int length) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < str.length(); i += length) {
            if (i + length > str.length()) {
                list.add(str.substring(i));
            } else {
                list.add(str.substring(i, i + length));
            }
        }
        return list;
    }

    @SneakyThrows
    public boolean notify(String content, List<String> limitPhoneNums) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode json = objectMapper.createObjectNode();
        json.put("content", content);
        if (limitPhoneNums != null && !limitPhoneNums.isEmpty()) {
            ArrayNode array = objectMapper.createArrayNode();
            ObjectNode subJson = objectMapper.createObjectNode();
            subJson.put("type", "mobiles");
            ArrayNode phoneNumsArray = objectMapper.valueToTree(limitPhoneNums);
            subJson.set("values", phoneNumsArray);
            array.add(subJson);
            json.set("notifyParams", array);
        }
        String jsonString = objectMapper.writeValueAsString(json);
        log.info("请求信息：{}", StringUtils.substring(jsonString, 0, 200));

        RequestBody body = RequestBody.create(jsonString, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                String resp = responseBody.string();
                log.info("云之家响应：{}", resp);
                JsonNode rootNode = new ObjectMapper().readTree(resp);
                if (rootNode.isObject()) {
                    ObjectNode objectNode = (ObjectNode) rootNode;
                    return objectNode.get("success").asBoolean();
                }
            }

        } catch (Exception e) {
            log.error("发送云之家异常：", e);
        }
        return false;
    }
}
