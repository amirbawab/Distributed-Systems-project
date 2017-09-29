class Client {
    private static uid : number = 0;
    public id : number = Client.uid++;
    public active : boolean = false;

    public getName() : string {
        return "Client " + this.id;
    }
    public getColor() : string {
        if(this.active) {
            return "#00a8ff";
        }
        return "#555";
    }
}