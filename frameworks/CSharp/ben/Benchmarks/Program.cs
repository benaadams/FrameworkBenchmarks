using System;
using Ben.Http;
using Microsoft.Net.Http.Headers;
using SqlConnection = Npgsql.NpgsqlConnection;

var connection = Environment.GetEnvironmentVariable("DB_CONNECTION");
var (server, app) = (new HttpServer("http://+:8080"), new HttpApp());

app.Get("/plaintext", () => "Hello, World!");

app.Get("/json", (req, res) => {
    res.Headers.ContentLength = 27;
    return res.Json(new Note { message = "Hello, World!" });
});

app.Get("/fortunes", async (req, res) => {
    using SqlConnection conn = new(connection);
    var model = await conn.QueryAsync<(int id, string message)>("SELECT id, message FROM fortune");
    model.Add((0, "Additional fortune added at request time."));
    model.Sort((x, y) => string.CompareOrdinal(x.message, y.message));
    res.Headers[HeaderNames.ContentType] = "text/html; charset=UTF-8";
    MustacheTemplates.RenderFortunes(model, res.Writer);
});

await server.RunAsync(app);

struct Note { public string message { get; set; } }