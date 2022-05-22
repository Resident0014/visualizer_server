import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.serialization.JavaParserJsonSerializer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import spark.Spark;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import java.io.StringWriter;
import java.util.HashMap;

public class Main {
    private static class AstResponse {
        private final String simple;
        private final String extended;

        public AstResponse(String simple, String extended) {
            this.simple = simple;
            this.extended = extended;
        }
    }

    public static void main(String[] args) {
        Gson gson = new Gson();

        Spark.after((request, response) -> {
            String origin = request
                    .headers("Origin");
            if (origin != null) {
                response.header("Access-Control-Allow-Origin", origin);
            }
            String accessControlRequestMethod = request
                    .headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods",
                        accessControlRequestMethod);
            }
            response.header("Access-Control-Allow-Methods", request.requestMethod());
        });

        Spark.options("/*", (request, response) -> {
            String accessControlRequestHeaders = request
                    .headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers",
                        accessControlRequestHeaders);
            }
            return "OK";
        });

        Spark.post("/java/ast", (request, response) -> {
            response.header("Content-Type", "application/json");
            JsonObject jsonObject = JsonParser.parseString(request.body()).getAsJsonObject();
            String srcCode = jsonObject.get("code").getAsString();
            MethodDeclaration md = StaticJavaParser.parseMethodDeclaration(srcCode);
            JsonPrinter printer = new JsonPrinter(true);
            String simple = JsonParser.parseString(printer.output(md)).toString();
            String extended = serialize(md);
            AstResponse data = new AstResponse(simple, extended);
            return new Response("success", data);
        }, gson::toJson);
    }

    static String serialize(Node node) {
        JsonGeneratorFactory generatorFactory = Json.createGeneratorFactory(new HashMap<>());
        JavaParserJsonSerializer serializer = new JavaParserJsonSerializer();
        StringWriter jsonWriter = new StringWriter();
        try (JsonGenerator generator = generatorFactory.createGenerator(jsonWriter)) {
            serializer.serialize(node, generator);
        }
        return jsonWriter.toString();
    }
}
