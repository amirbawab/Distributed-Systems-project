var clients = [];
function connect() {
    console.log("Connecting ...");
    newClient();
}

function newClient() {
    var newClient = new Client();
    clients.push(newClient);
    draw();
}

var a = 0;
function draw() {
    var canvas = document.getElementById("arch");
    var ctx = canvas.getContext("2d");
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Configure Client box
    var clientWidth = 100;
    var clientHeight = 50;
    var clientPadding = 10;

    // Configure Server box
    var serverWidth = 100;
    var serverHeight = 50;
    var serverPadding = 2 * (clientPadding + clientWidth);

    // Resize canvas
    canvas.width = 800;
    canvas.height = clients.length * (clientHeight + clientPadding);

    // Calculate server position
    var serverY = Math.max(0, (clients.length * (clientHeight+clientPadding) + clientPadding) / 2 - serverHeight/2);
    var serverX = serverPadding;

    // Draw server box
    ctx.fillStyle="#ff0089";
    ctx.fillRect(serverX, serverY, serverWidth, serverHeight);

    // Write server text
    ctx.fillStyle="#ffffff";
    ctx.font = "14px Arial";
    ctx.fillText("Mid-Server", serverX + serverWidth * 0.1, (serverY + serverY + serverHeight)/2);

    // Draw clients
    for(var i=0; i < clients.length; i++) {
        // Calculate client position
        var clientY = i*(clientHeight+clientPadding) + clientPadding;
        var clientX = clientPadding;

        // Draw box
        ctx.fillStyle=clients[i].getColor();
        ctx.fillRect(clientX, clientY, clientWidth, clientHeight);

        // Write text
        ctx.fillStyle="#ffffff";
        ctx.fillText(clients[i].getName(), clientX + clientWidth * 0.2, (clientY + clientY + clientHeight)/2);

        // Draw connection
        ctx.beginPath();
        ctx.moveTo(clientX + clientWidth, (clientY + clientY + clientHeight)/2);
        ctx.lineTo(serverX, serverY+(serverHeight/2));
        ctx.stroke();
    }
}

// Call draw function
draw();
