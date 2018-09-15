package data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Order {
    final String state;
    final String created_at;
    final String average_price;
    final String price;
    final String url;
    final String instrument;
    final String quantity;
}
