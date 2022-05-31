import com.binance.api.client.domain.OrderSide;
import me.ultimate.tvhook.utils.PlaceholderMap;
import me.ultimate.tvhook.utils.Utils;

public class Test {
    public static void main(String[] args) {
        PlaceholderMap map = PlaceholderMap.builder()
                .add("test", "test123")
                .add("test2", "test456")
                .add("red", String.valueOf(Utils.RED))
                .add("iscondition", "true");

        System.out.println(map.apply("test: {test} test2: {upper(test2)} red: {red}"));
        System.out.println(map.apply("{if(isCondition, thats true, thats false)}"));
        System.out.println("\n\n");

        PlaceholderMap map2 = PlaceholderMap.builder()
                .add("side", OrderSide.SELL.toString())
                .add("isbuy", "false")
                .add("quantity", "0.000578")
                .add("symbol", "BTCUSDT")
                .add("bal_fiat", "115.26")
                .add("fiat", "USDT");

        double p = 3.38;
        map2.add("profit", Utils.round(p, 2));
        map2.add("profit_color", p > 0 ? String.valueOf(Utils.GREEN) : String.valueOf(Utils.RED));
        map2.add("profit_percent", (p > 0 ? "+" : "-") + Utils.round(Math.abs(p / 115.26 * 100.0D), 2) + "%");

        System.out.println("Title: "+map2.apply("{if(isbuy, :open_circle:, :money_mouth:)} {upper(side)} Order Filled {if(isbuy, :open_circle:, :money_mouth:)}"));
        System.out.println("Desc: " + map2.apply("[{side}] {quantity} {symbol} ({bal_fiat} {fiat})\\n**Profit:** {profit} {fiat} (**{profit_percent}**)"));
        System.out.println("Col: " + map2.apply("{if(false, LIGHT_BLUE, profit_color)}"));
    }
}
