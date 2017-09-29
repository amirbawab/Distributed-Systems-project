var Client = (function () {
    function Client() {
        this.id = Client.uid++;
        this.active = false;
    }
    Client.prototype.getName = function () {
        return "Client " + this.id;
    };
    Client.prototype.getColor = function () {
        if (this.active) {
            return "#00a8ff";
        }
        return "#555";
    };
    Client.uid = 0;
    return Client;
})();
