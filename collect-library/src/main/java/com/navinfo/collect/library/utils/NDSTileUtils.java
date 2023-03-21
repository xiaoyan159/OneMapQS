package com.navinfo.collect.library.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.pow;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

@RequiresApi(api = Build.VERSION_CODES.N)
public class NDSTileUtils implements Serializable {

    /**
     * 根据经纬度坐标计算 dns瓦片号
     *
     * @param nLevel dns瓦片的级别
     * @param x      精度坐标
     * @param y      纬度坐标
     * @return 瓦片号
     */
    public static int getTileId(long nLevel, double x, double y) {
        int nTileID;
        int p;
        int nX = (int) (x / 90 * (1 << 30));
        int nY = (int) (y / 90 * (1 << 30));

        nTileID = nX < 0 ? 1 : 0;

        for (p = 30; p > (30 - nLevel); p--) {
            nTileID <<= 1;
            if ((nY & (1 << p)) != 0) {
                nTileID |= 1;
            }
            nTileID <<= 1;
            if ((nX & (1 << p)) != 0) {
                nTileID |= 1;
            }
        } 
        nTileID += (1 << (16 + nLevel));

        return nTileID;
    } 



    //根据nds 瓦片号 获取瓦片的级别
    public static int getTileLevel(int nTileID) {
        //1<<(16+ nLevel) 最高位，反推
        int t = 31;
        while ((nTileID & (1 << t)) == 0) {
            t--;
        }
        return t - 16;
    }

    //4.输入tileid，和方向，输出它接边的tile
    public static int getNeighborTile(int nTileID, String enumPos) {
        int i;
        int nLevel = getTileLevel(nTileID);
        if ("UP".equals(enumPos)) {// y + 1;
            for (i = 0; i < nLevel; i++) {
                if ((nTileID & (1 << ((i << 1) + 1))) == 0) {
                    nTileID |= 1 << ((i << 1) + 1);
                    return nTileID;
                } else {
                    nTileID &= ~(1 << ((i << 1) + 1));
                }
            }
        } else if ("DOWN".equals(enumPos)) {// y - 1;
            for (i = 0; i < nLevel; i++) {
                if ((nTileID & (1 << ((i << 1) + 1))) == 0) {
                    nTileID |= 1 << ((i << 1) + 1);
                } else {
                    nTileID &= ~(1 << ((i << 1) + 1));
                    return nTileID;
                }
            }
        } else if ("LEFT".equals(enumPos)) {// x - 1
            for (i = 0; i < nLevel; i++) {
                if ((nTileID & (1 << (i << 1))) == 0) {
                    nTileID |= 1 << (i << 1);
                } else {
                    nTileID &= ~(1 << (i << 1));
                    return nTileID;
                }
            }
        } else if ("RIGHT".equals(enumPos)) {// x + 1
            for (i = 0; i < nLevel; i++) {
                if ((nTileID & (1 << (i << 1))) == 0) {
                    nTileID |= 1 << (i << 1);
                    return nTileID;
                } else {
                    nTileID &= ~(1 << (i << 1));
                }
            }
        }
        System.out.println("Failed to get neighbor tile: " + nTileID + enumPos);
        return 0;
    }


    //5. 输入Tileid，输出该tile的geomtry信息
    public static LineString getTileBundingBoxGeo(int m_tile_id) {

        long left = 0;
        long bottom = 0;
        //
        int m_lb_x;
        int m_lb_y;
        int m_rt_x;
        int m_rt_y;
        int m_level = getTileLevel(m_tile_id);

        List<String> mc = getBit_left(m_tile_id, m_level);

        for (short i = 0; i < 31; ++i) {
            left = setBit(left, i, getBit_left(mc, (i << 1)));
            bottom = setBit(bottom, i, getBit_left(mc, (i << 1) + 1));
        }
        left = setBit(left, 31, getBit_left(mc, 62));

        m_lb_x = (int) (left >> (31 - m_level));
        if ((0x40000000 & bottom) != 0) {
            bottom |= (long) (0x80000000);
        }
        m_lb_y = (int) (bottom >> (31 - m_level));

        m_lb_x = (int) ((long) (m_lb_x) << (31 - m_level));
        m_lb_y = (int) ((long) (m_lb_y) << (31 - m_level));
        m_rt_x = m_lb_x + getTileWidth(m_level);
        m_rt_y = m_lb_y + getTileHeight(m_level);

        StringBuilder sb = new StringBuilder("LineString (");
        double lb_x = m_lb_x * 90.0 / (1 << 30); //左下经度
        double lb_y = m_lb_y * 90.0 / (1 << 30); //左下纬度
        double rt_x = m_rt_x * 90.0 / (1 << 30); //右上经度
        double rt_y = m_rt_y * 90.0 / (1 << 30); //右上纬度
        sb.append(lb_x);

        sb.append(" ");

        sb.append(lb_y);

        sb.append(",");

        sb.append(rt_x);

        sb.append(" ");

        sb.append(lb_y);

        sb.append(",");

        sb.append(rt_x);

        sb.append(" ");

        sb.append(rt_y);

        sb.append(",");

        sb.append(lb_x);

        sb.append(" ");

        sb.append(rt_y);

        sb.append(",");

        sb.append(lb_x);

        sb.append(" ");

        sb.append(lb_y);

        sb.append(")");

        LineString line = null;
        WKTReader reader = new WKTReader();
        Geometry read;
        try {
            read = reader.read(sb.toString());

            line = (LineString) read;

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return line;
    }

    //5. 输入Tileid，输出该tile的geomtry信息
    public static Polygon getTilePolygonGeo(int m_tile_id) {

        long left = 0;
        long bottom = 0;
        //
        int m_lb_x;
        int m_lb_y;
        int m_rt_x;
        int m_rt_y;
        int m_level = getTileLevel(m_tile_id);

        List<String> mc = getBit_left(m_tile_id, m_level);

        for (short i = 0; i < 31; ++i) {
            left = setBit(left, i, getBit_left(mc, (i << 1)));
            bottom = setBit(bottom, i, getBit_left(mc, (i << 1) + 1));
        }
        left = setBit(left, 31, getBit_left(mc, 62));

        m_lb_x = (int) (left >> (31 - m_level));
        if ((0x40000000 & bottom) != 0) {
            bottom |= (long) (0x80000000);
        }
        m_lb_y = (int) (bottom >> (31 - m_level));

        m_lb_x = (int) ((long) (m_lb_x) << (31 - m_level));
        m_lb_y = (int) ((long) (m_lb_y) << (31 - m_level));
        m_rt_x = m_lb_x + getTileWidth(m_level);
        m_rt_y = m_lb_y + getTileHeight(m_level);

        StringBuilder sb = new StringBuilder("POLYGON ((");
        double lb_x = m_lb_x * 90.0 / (1 << 30); //左下经度
        double lb_y = m_lb_y * 90.0 / (1 << 30); //左下纬度
        double rt_x = m_rt_x * 90.0 / (1 << 30); //右上经度
        double rt_y = m_rt_y * 90.0 / (1 << 30); //右上纬度
        sb.append(lb_x);

        sb.append(" ");

        sb.append(lb_y);

        sb.append(",");

        sb.append(rt_x);

        sb.append(" ");

        sb.append(lb_y);

        sb.append(",");

        sb.append(rt_x);

        sb.append(" ");

        sb.append(rt_y);

        sb.append(",");

        sb.append(lb_x);

        sb.append(" ");

        sb.append(rt_y);

        sb.append(",");

        sb.append(lb_x);

        sb.append(" ");

        sb.append(lb_y);

        sb.append("))");

        Polygon polygon = null;
        WKTReader reader = new WKTReader();
        Geometry read;
        try {
            read = reader.read(sb.toString());

            polygon = (Polygon) read;

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return polygon;
    }


    public static List<String> getBit_left(int m_tile_id, int m_level) {
        //unsigned long long mc = (unsigned long long)(m_tile_id) << (63 - ((m_level << 1) + 1));
        /*
         * long long 64位
         * */

        //计算需要位移的个数
        int cnt_move = (63 - ((m_level << 1) + 1));
        //将tileid转成自定的list
        String tileid = toBinary(m_tile_id, 31);
        List<String> list =
                IntStream.range(0, tileid.length()).mapToObj(i -> String.valueOf(tileid.charAt(i))).collect(Collectors.toList()); //定义对象依次存放每一个字符，现在里面是31个字符

        //String[] str1 = new String[31];

        List<String> list_new = new ArrayList<>(); //定义对象依次存放每一个字符，现在里面是31个字符
        //左移cnt_move个，数组的最后cnt_move个0
        //18446744073709551615+1  unsigned long long 的最大值
        //因为总共是64位，所以当list个数+cnt_move不超过64的时候，直接在后面加0即可
        if (list.size() + cnt_move < 65) {
            IntStream.range(0, cnt_move).mapToObj(i -> "0").forEach(list::add);

            //如果不满足64位，前面补位
            int addcnt = 64 - list.size();
            list_new = IntStream.range(0, addcnt).mapToObj(i -> "0").collect(Collectors.toList());
            list_new.addAll(list);
        } else {//当list个数+cnt_move超出64
            IntStream.range(0, cnt_move).mapToObj(i -> "0").forEach(list::add);
            int delcnt = list.size() - 64;
            for (int i = 0; i < list.size(); i++) {
                if (i >= delcnt) {
                    list_new.add(list.get(i));
                }
            }
        }
        return list_new;
    }


    /**
     * 将一个int数字转换为二进制的字符串形式。
     *
     * @param num    需要转换的int类型数据
     * @param digits 要转换的二进制位数，位数不足则在前面补0
     * @return 二进制的字符串形式
     */
    public static String toBinary(int num, int digits) {
        int value = 1 << digits | num;
        String bs = Integer.toBinaryString(value); //0x20 | 这个是为了保证这个string长度是6位数
        return bs.substring(1);
    }

    public static long setBit(long a, int b, boolean flag) {
        return flag ? (a |= ((1L) << b)) : (a &= ~((1L) << b));
    }

    public static int calcLevel(int m_tile_id) {
        int level = 0;
        for (short i = 0; i < 16; ++i) {
            long mask = ((long) (0x80000000) >> i);
            if (((long) (m_tile_id) & mask) != 0) {
                level = 15 - i;
                break;
            }
        }
        return (level);
    }

    public static int getTileWidth(int lv) {
        return (int) ((long) (4294967296L) >> (lv + 1));
    }

    public static int getTileHeight(int lv) {
        return (int) ((long) (2147483648L) >> (lv));
    }

    public static boolean getBit_left(List<String> a, int b) {
        //(1L) << b) 先算出这一块的二进制，用list装.31位。
        List<String> list_level;

        String c = toBinary(1, 31);
        list_level = IntStream.range(0, 33).mapToObj(i -> "0").collect(Collectors.toList());
        IntStream.range(0, c.length()).mapToObj(i -> String.valueOf(c.charAt(i))).forEach(list_level::add);

        List<String> list_level_new;
        IntStream.range(0, b).mapToObj(i -> "0").forEach(list_level::add);

        int cnt = list_level.size() - 64;
        list_level_new =
                IntStream.range(0, list_level.size()).filter(i -> i >= cnt).mapToObj(list_level::get).collect(Collectors.toList());

        //开始位&运算，目前两个list都是64位的几何
        return IntStream.range(0, a.size()).anyMatch(i -> "1".equals(list_level_new.get(i)) && "1".equals(a.get(i)));
    }

    //按先下后左的原则，计算点 线 面 的最低点
    public static Point getNadir(Geometry geometry) throws Exception {

        String geometryType = geometry.getGeometryType();
        if ("Point".equals(geometryType)) {
            return (Point) geometry;
        }
        if ("LineString".equals(geometryType)) {
            double coor_y = 99999999.0;
            double coor_x = 99999999.0;
            LineString lineString = (LineString) geometry;
            Coordinate[] coordinates = lineString.getCoordinates();

            for (Coordinate coordinate : coordinates) {
                double y = coordinate.y;
                if (y < coor_y) {
                    coor_y = y;
                    coor_x = coordinate.x;
                } else {
                    if (y == coor_y) {
                        double x = coordinate.x;
                        if (x < coor_x) {
                            coor_x = x;
                        }
                    }
                }
            }
            WKTReader reader = new WKTReader();
            return (Point) reader.read("POINT(" + coor_x + " " + coor_y + ")");
        }
        if ("POLYGON".equals(geometryType)) {
            double coor_y = 99999999.0;
            double coor_x = 99999999.0;
            Polygon polygon = (Polygon) geometry;
            Coordinate[] coordinates = polygon.getCoordinates();
            for (int i = 0; i < coordinates.length; i++) {
                double y = coordinates[i].y;
                if (y < coor_y) {
                    coor_y = y;
                    coor_x = coordinates[i].x;
                } else {
                    if (y == coor_y) {
                        double x = coordinates[i].x;
                        if (x < coor_x) {
                            coor_x = x;
                        }
                    }
                }
            }
            WKTReader reader = new WKTReader();
            return (Point) reader.read("POINT(" + coor_x + ", " + coor_y + ")");
        }
        return null;
    }


    /**
     * 基于here 、wgs84坐标系
     * 获取一个瓦片的左下角、右上角坐标
     * <p>
     * 左下角x:MinLon:double[0]
     * 左下角y:MinLatdouble[1]
     * 右下角x:MaxLon:double[2]
     * 右下角x:MaxLat:double[3]
     * <p>
     * 算法来源：
     * https://developer.here.com/documentation/platform-data/dev_guide/topics/layers-indexes-attributes.html
     *
     * @param tileId 瓦片号码
     * @return 瓦片左下角、右上角坐标数组
     */
    public static double[] GetTileBoundary(int tileId) {
        double[] result = new double[4];
        String[] mortonCode = new StringBuilder(Integer.toBinaryString(tileId)).reverse().toString().split("");
        String binaryX = "";
        String binaryY = "";

        for (int i = 0; i < mortonCode.length; i++) {
            if (i % 2 == 0) {
                binaryX = mortonCode[i] + binaryX;
            } else {
                binaryY = mortonCode[i] + binaryY;
            }
        }
        //计算得出瓦片行号和列号
        int tileX = Integer.valueOf(binaryX, 2);
        int tileY = Integer.valueOf(binaryY, 2);

        double tileSize = 180 / pow(2, getTileLevel(tileId));

        /**
         * 只支持北半球中国地区，如果支持全球瓦片，根据坐标（-）做如下处理，例如如果南半球tileid需要 -90.0
         */
        double tileMinLon = tileX * tileSize;
        double tileMinLat = tileY * tileSize;
        double tileMaxLon = tileMinLon + tileSize;
        double tileMaxLat = tileMinLat + tileSize;


        result[0] = tileMinLon;
        result[1] = tileMinLat;
        result[2] = tileMaxLon;
        result[3] = tileMaxLat;

        return result;
    }

    //根据点的坐标 获取对应级别的partitionName
    public static int getPartitionCoordinate(long nLevel, double x, double y) {
        int nTileID;
        int p;
        int nX = (int) (x / 90 * (1 << 30));
        int nY = (int) (y / 90 * (1 << 30));

        nTileID = nX < 0 ? 1 : 0;

        for (p = 30; p > (30 - nLevel); p--) {
            nTileID <<= 1;
            if ((nY & (1 << p)) != 0) {
                nTileID |= 1;
            }
            nTileID <<= 1;
            if ((nX & (1 << p)) != 0) {
                nTileID |= 1;
            }
        }
        nTileID += (1 << (16 + nLevel));

        return nTileID;
    }

    //根据瓦片号算出周边8个瓦片
    public static List<Integer> getSurroundTile(int nTileID) {

        ArrayList<Integer> resList = new ArrayList<Integer>();
        //左方向瓦片
        int left = getNeighborTile(nTileID, "LEFT");
        if (left != 0) {
            resList.add(left);
        }

        //左上方瓦片
        int upperLeft = getNeighborTile(left, "UP");
        if (upperLeft != 0) {
            resList.add(upperLeft);
        }

        //正上方瓦片
        int up = getNeighborTile(nTileID, "UP");
        if (up != 0) {
            resList.add(up);
        }

        //右上方瓦片
        int upperRight = getNeighborTile(up, "RIGHT");
        if (upperRight != 0) {
            resList.add(upperRight);
        }

        //右瓦片
        int right = getNeighborTile(nTileID, "RIGHT");
        if (right != 0) {
            resList.add(right);
        }

        //右下瓦片
        int lowerRight = getNeighborTile(right, "DOWN");
        if (lowerRight != 0) {
            resList.add(lowerRight);
        }

        //正下瓦片
        int down = getNeighborTile(nTileID, "DOWN");
        if (down != 0) {
            resList.add(down);
        }

        //左下瓦片
        int downLeft = getNeighborTile(down, "LEFT");
        if (downLeft != 0) {
            resList.add(downLeft);
        }
        return resList;
    }

    //给一个面，算出相交的所有的paritionName
    public static List<Integer> getPartitionNameByPolygon(long level, Polygon polygon) throws Exception {

        if (level > 16) {
            throw new Exception("NDS 瓦片号不能超过16级！");
        }
        Geometry boundary = polygon.getBoundary().getEnvelope();

        List<Integer> list = new ArrayList<>();

        Coordinate[] coordinates = boundary.getCoordinates();
        //最低点  最高点
        double lowX = coordinates[0].x;
        double lowY = coordinates[0].y;
        double highX = coordinates[2].x;
        double highY = coordinates[2].y;

        //最低点所属瓦片
        int partitionName = getPartitionCoordinate(level, lowX, lowY);

        //获取瓦片的几何信息
        List<Double> tileBundingBoxCoor = getTileBundingBoxCoor(partitionName);

        //瓦片的长宽
        double lengh = tileBundingBoxCoor.get(2) - tileBundingBoxCoor.get(0);
        double wide = tileBundingBoxCoor.get(3) - tileBundingBoxCoor.get(1);

        Set<Integer> set = new HashSet<>();

        int count = 0;
        int partition = 0;
        for (double i = lowX; i < highX + lengh; i = i + lengh) {
            if (i == lowX) {
                partition = partitionName;
            } else {
                partition = getNeighborTile(partition, "RIGHT");
            }
            set.add(partition);
            for (double j = lowY; j < highY + wide; j = j + wide) {
                if (count % 2 == 0) {
                    partition = getNeighborTile(partition, "UP");
                } else {
                    partition = getNeighborTile(partition, "DOWN");
                }
                set.add(partition);
            }
            count++;
        }

        set.forEach(e -> {
            Polygon tileBoxGeo = getTilePolygonGeo(e);

            if (tileBoxGeo.intersects(polygon) && tileBoxGeo.intersection(polygon).getCoordinates().length > 1) {
                list.add(e);
            }
        });

        return list;
    }


    public static List<Integer> getPartitionNameByMultiPolygon(long level, MultiPolygon multiPolygon) throws Exception {

        ArrayList<Integer> partitionNames = new ArrayList<>();
        int numGeo = multiPolygon.getNumGeometries();

        for (int i = 0; i < numGeo; i++) {
            Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);

            List<Integer> partitionNameByPolygon = getPartitionNameByPolygon(level, polygon);

            partitionNames.addAll(partitionNameByPolygon);
        }

        return partitionNames;
    }

    //给一条线，算出相交的所有的paritionName
    public static Set<Integer> getPartitionNameByLineString(long level, LineString lineString) throws Exception {

        Set<Integer> set = new HashSet<>();
        //包络线的顺序是按坐标从小到大排好的
        Geometry envelope = lineString.getEnvelope();
        String geometryType = envelope.getGeometryType();

        if ("Polygon".equals(geometryType)) {
            Polygon polygon = (Polygon) envelope;

            Coordinate[] coordinates = polygon.getCoordinates();
            double mixX = coordinates[0].x;
            double mixY = coordinates[0].y;
            double maxX = coordinates[2].x;
            double maxY = coordinates[2].y;

            int mixPartition = getPartitionCoordinate(level, mixX, mixY);

            //获取瓦片的几何信息
            List<Double> tileBundingBoxCoor = getTileBundingBoxCoor(mixPartition);

            //瓦片的长宽
            double lengh = tileBundingBoxCoor.get(2) - tileBundingBoxCoor.get(0);
            double wide = tileBundingBoxCoor.get(3) - tileBundingBoxCoor.get(1);

            int partition;

            //先通过遍历找出该矩形区域中所有的partiionname
            Set<Integer> partitionSet = new HashSet<>();
            for (double i = mixX; i < maxX + lengh; i = i + lengh) {
                if (i != mixX) {
                    mixPartition = getNeighborTile(mixPartition, "RIGHT");
                }
                partition = mixPartition;
                partitionSet.add(partition);

                for (double j = mixY; j < maxY + wide; j = j + wide) {

                    partition = getNeighborTile(partition, "UP");

                    partitionSet.add(partition);
                }
            }
            partitionSet.forEach(e -> {
                Polygon tileBoxGeo = getTilePolygonGeo(e);

                if (tileBoxGeo.intersects(lineString) && tileBoxGeo.intersection(lineString).getCoordinates().length > 1) {
                    set.add(e);
                }
            });

            return set;
        }

        //说明line是一条水平线 或者只有两个点
        if ("Linestring".equals(geometryType)) {
            LineString line = (LineString) envelope;

            Coordinate[] coordinates = line.getCoordinates();
            double mixX = Math.min(coordinates[0].x, coordinates[1].x);
            double mixY = Math.min(coordinates[0].y, coordinates[1].y);
            double maxX = Math.max(coordinates[1].x, coordinates[0].x);
            double maxY = Math.max(coordinates[1].y, coordinates[0].y);

            int mixPartition = getPartitionCoordinate(level, mixX, mixY);
            //获取瓦片的几何信息
            List<Double> tileBundingBoxCoor = getTileBundingBoxCoor(mixPartition);

            //瓦片的长宽
            double lengh = tileBundingBoxCoor.get(2) - tileBundingBoxCoor.get(0);
            double wide = tileBundingBoxCoor.get(3) - tileBundingBoxCoor.get(1);

            int partition = mixPartition;
            if (mixY == maxY) {
                for (double i = mixX; i < maxX + lengh; i = i + lengh) {
                    LineString tileBoxGeo = getTileBundingBoxGeo(partition);
                    if (tileBoxGeo.intersects(lineString)) {
                        set.add(partition);
                        partition = getNeighborTile(partition, "RIGHT");
                    }
                }
            }
            if (mixX == maxX) {
                for (double i = mixX; i < maxX + wide; i = i + wide) {
                    LineString tileBoxGeo = getTileBundingBoxGeo(partition);
                    if (tileBoxGeo.intersects(lineString)) {
                        set.add(partition);
                        partition = getNeighborTile(partition, "UP");
                    }
                }
            }
            return set;
        }

        if ("Point".equals(geometryType)) {
            Point point = (Point) envelope;
            int tileId = getTileId(level, point.getX(), point.getY());
            set.add(tileId);
            return set;
        }
        return set;
    }


//    //给一条线，算出相交的所有的paritionName
//    public static Set<Integer> getPartitionNameByLineString(long level, LineString lineString) throws Exception {
//
//        Set<Integer> set = new HashSet<>();
//        //不要直接get包络线，当是一条水平或者垂直直线时会报错
//        Geometry envelope = lineString.getBoundary().getEnvelope();
//        String geometryType = envelope.getGeometryType();
//
//        //如果是水平或者垂直直线，envelope只会有两个点
//        Coordinate[] coordinates = envelope.getCoordinates();
//
//        double mixX = Math.min(coordinates[0].x, coordinates[1].x);
//        double mixY = Math.min(coordinates[0].y, coordinates[1].y);
//        double maxX = Math.max(coordinates[1].x, coordinates[0].x);
//        double maxY = Math.max(coordinates[1].y, coordinates[0].y);
//
//        int mixPartition = getPartitionCoordinate(level, mixX, mixY);
//        //获取瓦片的几何信息
//        List<Double> tileBundingBoxCoor = getTileBundingBoxCoor(mixPartition);
//
//        //瓦片的长宽
//        double lengh = tileBundingBoxCoor.get(2) - tileBundingBoxCoor.get(0);
//        double wide = tileBundingBoxCoor.get(3) - tileBundingBoxCoor.get(1);
//
//        //水平
//        int partition = mixPartition;
//        if (mixY == maxY) {
//            for (double i = mixX; i < maxX + lengh; i = i + lengh) {
//                LineString tileBoxGeo = getTileBundingBoxGeo(partition);
//                if (tileBoxGeo.intersects(lineString)) {
//                    set.add(partition);
//                    partition = getNeighborTile(partition, "RIGHT");
//                }
//            }
//        }
//        if (mixX == maxX) {
//            for (double i = mixX; i < maxX + wide; i = i + wide) {
//                LineString tileBoxGeo = getTileBundingBoxGeo(partition);
//                if (tileBoxGeo.intersects(lineString)) {
//                    set.add(partition);
//                    partition = getNeighborTile(partition, "UP");
//                }
//            }
//        } else {
//            Polygon boundary = (Polygon) lineString.getEnvelope();
//            List<Integer> partitionNameByPolygon = getPartitionNameByPolygon(level, boundary);
//            set.addAll(partitionNameByPolygon);
//        }
//
//        return set;
//    }

    public static Set<Integer> getPartitionNameByMultiLineString(long level, MultiLineString multiLineString) throws Exception {
        Set<Integer> partitionNames = new HashSet<>();
        int numGeo = multiLineString.getNumGeometries();

        for (int i = 0; i < numGeo; i++) {
            LineString lineString = (LineString) multiLineString.getGeometryN(i);

            Set<Integer> partitionNameByLineString = getPartitionNameByLineString(level, lineString);

            partitionNames.addAll(partitionNameByLineString);
        }

        return partitionNames;
    }

    public static Set<Integer> getPartitionNameByMultiPoint(long level, MultiPoint multiPoint) throws Exception {

        Set<Integer> partitionNames = new HashSet<>();

        int numGeo = multiPoint.getNumGeometries();

        for (int i = 0; i < numGeo; i++) {
            Point point = (Point) multiPoint.getGeometryN(i);

            Integer partitionNameByPoint = getPartitionNameByPoint(level, point);

            partitionNames.add(partitionNameByPoint);
        }

        return partitionNames;
    }

    public static Integer getPartitionNameByPoint(long level, Point point) {
        return getPartitionCoordinate(level, point.getX(), point.getY());
    }

    /*//给点和半径，算出与圆相交的所有的paritionName
    public static List<Integer> getPartitionNameByCircle(long level, double x, double y, double radius) throws
            Exception {
        Polygon circle = createCircle(x, y, radius);
        return getPartitionNameByPolygon(level, circle);
    }*/

    //算出某个瓦片的左下瓦片
    private static int getLeftDownTile(int tileId) {
        //正下瓦片
        int down = getNeighborTile(tileId, "DOWN");
        //左下瓦片
        if (down != 0) {
            return getNeighborTile(down, "LEFT");
        }
        return 0;
    }

    // 输入Tileid，输出该tile的四个点的geomtry信息
    public static List<Double> getTileBundingBoxCoor(int m_tile_id) {

        long left = 0;
        long bottom = 0;
        //
        int m_lb_x = 0;
        int m_lb_y = 0;
        int m_rt_x = 0;
        int m_rt_y = 0;
        int m_level = getTileLevel(m_tile_id);

        List<String> mc = getBit_left(m_tile_id, m_level);

        for (short i = 0; i < 31; ++i) {
            left = setBit(left, i, getBit_left(mc, (i << 1)));
            bottom = setBit(bottom, i, getBit_left(mc, (i << 1) + 1));
        }
        left = setBit(left, 31, getBit_left(mc, 62));

        m_lb_x = (int) (left >> (31 - m_level));
        if ((0x40000000 & bottom) != 0) {
            bottom |= (long) (0x80000000);
        }
        m_lb_y = (int) (bottom >> (31 - m_level));

        m_lb_x = (int) ((long) (m_lb_x) << (31 - m_level));
        m_lb_y = (int) ((long) (m_lb_y) << (31 - m_level));
        m_rt_x = m_lb_x + getTileWidth(m_level);
        m_rt_y = m_lb_y + getTileHeight(m_level);

        double lb_x = m_lb_x * 90.0 / (1 << 30); //左下经度
        double lb_y = m_lb_y * 90.0 / (1 << 30); //左下纬度
        double rt_x = m_rt_x * 90.0 / (1 << 30); //右上经度
        double rt_y = m_rt_y * 90.0 / (1 << 30); //右上纬度

        List<Double> list = new ArrayList<>();
        list.add(lb_x);
        list.add(lb_y);
        list.add(rt_x);
        list.add(rt_y);

        return list;
    }

    /**
     * 根据某个瓦片扩圈
     *
     * @param tileId 要扩圈的瓦片编号
     * @param number 扩圈的次数
     * @return 扩圈完后，所有瓦片的集合
     */
    public static Set<Integer> getSurroundTile(int tileId, int number) {
        Set<Integer> set = new HashSet<>();

        int startTileId = tileId;
        int num = number;
        while (num >= 1) {
            startTileId = getLeftDownTile(startTileId);
            num--;
        }

        int count = 0;
        int partitionName = 0;
        for (int i = 0; i < number * 2 + 1; i++) {
            if (i == 0) {
                partitionName = startTileId;
            } else {
                partitionName = getNeighborTile(partitionName, "RIGHT");
            }
            set.add(partitionName);
            for (int j = 0; j < number * 2; j++) {
                if (count % 2 == 0) {
                    partitionName = getNeighborTile(partitionName, "UP");
                } else {
                    partitionName = getNeighborTile(partitionName, "DOWN");
                }
                set.add(partitionName);
            }
            count++;
        }

        return set;
    }

//    /**
//     * 根据partitionName 获取partition的geometry
//     *
//     * @param PartitionName
//     * @return wkt类型的 LineString
//     */
//    public static JSONObject getPartitionGeo(String PartitionName) throws Exception {
//
//        int intPartitionName = Integer.parseInt(PartitionName);
//
//        Geometry lineString = getTileBundingBoxGeo(intPartitionName);
//
//        //将wkt类型的字符串转成 geojson类型
//        return GeoTranslator.geometry2Geojson(lineString);
//
//    }

    /**
     * 根据partitionName 获取partition的geometry
     *
     * @param PartitionName
     * @return wkt类型的 LineString
     */
    public static LineString getPartitionWktGeo(String PartitionName) {

        int intPartitionName = Integer.parseInt(PartitionName);

        return getTileBundingBoxGeo(intPartitionName);

    }

    /*public static void main(String[] args) throws Exception {
//        WKTReader reader = new WKTReader();
//        Polygon polygon = (Polygon) reader.read("POLYGON ((73 18.19, 73 54, 135 54, 135 18.19, 73 18.19))");
//
//        //Polygon polygon = (Polygon) reader.read("POLYGON ((116.2394 39.8037,116.2394 39.8654, 116.3246 39.8654, 116.3246 39.8037, 116.2394 39.8037))");
//        List<Integer> partitionNameByPolygon = getPartitionNameByPolygon(13, polygon);
//
//        // JSONArray array = JSONArray.parseArray(JSON.toJSONString(partitionNameByPolygon));
//        System.out.println("partition计算完成");
//        List<Column> mifStruct = new ArrayList<>();
//        Column column = new Column();
//        column.setName("kind");
//        column.setType(Column.getColumnTypeChar(60));
//        mifStruct.add(column);
//
//        List<String[]> data = new ArrayList<>();
//        for (Integer partitionName : partitionNameByPolygon) {
//            List<String> dataStr = new ArrayList<>();
//
//            dataStr.add("line");
//            String geo= getTileBundingBoxGeo(partitionName).toString();
//            dataStr.add(geo);
//
//            String[] strs = dataStr.toArray(new String[0]);
//
//            data.add(strs);
//        }
//
//        System.out.println("开始写文件");
//        MifOutput mifOutput = new MifOutput("C:\\Users\\admin\\Desktop", "partition");
//        mifOutput.write(mifStruct, data);

        // writeFile("C:\\Users\\admin\\Desktop\\partition.txt", array);

//        String str="{\"coordinates\":[[[115.78742,28.70263],[115.78744,28.70264],[115.78791,28.70262],[115
//        .78796,28" +
////                ".70262],[115.78798,28.70261],[115.78799,28.70259],[115.78795,28.70223],[115.78794,28.70221],[115" +
////                ".78792,28.7022],[115.78745,28.70219],[115.78743,28.7022],[115.78742,28.70222],[115.78741,28
// .70261]," +
////                "[115.78742,28.70263]]],\"type\":\"Polygon\"}";
////
////        JSONObject jsonObject = JSONObject.parseObject(str);
////
////        Geometry geometry = GeoTranslator.geoJsonToGeo(jsonObject);
////        List<Integer> partitionNameByPolygon1 = getPartitionNameByPolygon(13, (Polygon) geometry);
////
////        for (Integer integer : partitionNameByPolygon1) {
////            System.out.println(integer);
////        }

//        Polygon tilePolygonGeo = getTilePolygonGeo(566666666);
//        System.out.println(tilePolygonGeo);

        WKTReader reader = new WKTReader();
        Geometry read = reader.read("LINESTRING (116.3671875 39.7705078125, 116.38916015625 39.7705078125, 116.38916015625 39.79248046875, 116.3671875 39.79248046875, 116.3671875 39.7705078125)");

        String s = GeoTranslator.geoToGeoJson(read);
        System.out.println(s);

        JSONObject jsonObject = GeoTranslator.geometry2Geojson(read);
        System.out.println(jsonObject);
        //partitionNameByPolygon.forEach(e-> System.out.println(e));

    }*/
}
