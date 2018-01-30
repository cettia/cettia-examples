var cettia = require("cettia-client");
var socket = cettia.open("http://localhost:8080/cettia", {reconnect: false});

// Lifecycle events
// When the selected transport starts connecting to the server
socket.on("connecting", function() {
    console.log("on connecting");
});
// When the server issues a new id for this socket as the beginning of the lifecycle and the end of the previous lifecycle
socket.on("new", function() {
  console.log("on new");
});
// When the connection is established successfully
socket.on("open", function() {
  console.log("on open");
});
// When an error happens on the socket
socket.on("error", function(error) {
  console.error("on error", error);
});
// When the connection is closed, regarded as closed or could not be opened
socket.on("close", function() {
  console.log("on close");
});
// When a reconnection is scheduled
socket.on("waiting", function(delay, attempts) {
  console.log("on waiting", attempts, delay);
});

// echo and chat events
socket.on("open", function() {
  // Text data
  socket.send("echo", "echo");
  socket.send("chat", "chat");
  // Binary data
  socket.send("echo", new Buffer("echo"));
  socket.send("chat", new Buffer("chat"));
  // Composite data
  socket.send("echo", {text: "echo", binary: new Buffer("echo")});
  socket.send("chat", {text: "chat", binary: new Buffer("chat")});
});
socket.on("echo", function(data) {
  console.log("on echo", data);
});
socket.on("chat", function(data) {
  console.log("on chat", data);
});
