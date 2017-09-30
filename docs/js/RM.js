var RM = (function () {
    function RM(name) {
        this.active = false;
        this.name = name;
    }
    RM.prototype.getName = function () {
        return this.name;
    };
    RM.prototype.getColor = function () {
        if (this.active) {
            return "#0c9900";
        }
        else {
            return "#555";
        }
    };
    RM.prototype.getTextColor = function () {
        return "#fff";
    };
    return RM;
})();
