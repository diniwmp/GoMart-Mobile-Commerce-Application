package lk.zenova.gomart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Address {
    private String documentId;
    private String address;
    private String addressName;
    private double latitude;
    private double longitude;
    private String userId;
    private boolean isDefault;



}