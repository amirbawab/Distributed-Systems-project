class RM {
    public name : string;
    public active : boolean = false;
    public constructor (name : string) {
        this.name = name;
    }
    public getName() : string {
        return this.name;
    }
    public getColor() : string {
        if(this.active) {
            return "#0c9900";
        } else {
            return "#555";
        }
    }
    public getTextColor(){
        return "#fff";
    }
}