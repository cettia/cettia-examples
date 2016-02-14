var cettia = require("cettia-client");
var socket = cettia.open("http://localhost:8080/cettia", {reconnect: false});
socket.on("connecting", function () {
  console.log("on connecting event");
})
.on("open", function () {
  console.log("on open event");
  socket.send("echo", "An echo message").send("chat", "A chat message");
})
.on("error", function (error) {
  console.log("on error event", error);
})
.on("close", function () {
  console.log("on close event");
})
.on("chat", function (data) {
  console.log("on chat event", data);
})
.on("echo", function (data) {
  console.log("on echo event", data);
});
