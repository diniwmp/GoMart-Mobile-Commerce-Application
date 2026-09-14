package lk.zenova.gomart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Brand {
    private String brandId;
    private String brandName;
    private String imageUrl;
}
