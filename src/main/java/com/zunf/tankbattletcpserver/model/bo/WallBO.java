package com.zunf.tankbattletcpserver.model.bo;

import com.zunf.tankbattletcpserver.enums.MapIndex;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WallBO {
    private MapIndex type;

    /**
     * 墙的坐标 非px
     */
    private int x;
    private int y;
}
