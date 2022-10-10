package net.srv;
import net.api.bidi.BidiProtocol;
import net.api.encdec;

public class ReactorMain {
    public static void main(String[] args) {
        Reactor<String> reactor = new Reactor<String>(Integer.decode(args[1]).intValue(),
                Integer.decode(args[0]).intValue(), //port
                () -> new BidiProtocol(),
                () -> new encdec());
        reactor.serve();
    }
}
