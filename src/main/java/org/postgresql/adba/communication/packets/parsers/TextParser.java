package org.postgresql.adba.communication.packets.parsers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.stream.Collectors;
import jdk.incubator.sql2.SqlException;
import org.postgresql.adba.pgdatatypes.Box;
import org.postgresql.adba.pgdatatypes.Circle;
import org.postgresql.adba.pgdatatypes.IntegerRange;
import org.postgresql.adba.pgdatatypes.Line;
import org.postgresql.adba.pgdatatypes.LineSegment;
import org.postgresql.adba.pgdatatypes.LocalDateRange;
import org.postgresql.adba.pgdatatypes.LocalDateTimeRange;
import org.postgresql.adba.pgdatatypes.LongRange;
import org.postgresql.adba.pgdatatypes.NumericRange;
import org.postgresql.adba.pgdatatypes.OffsetDateTimeRange;
import org.postgresql.adba.pgdatatypes.Path;
import org.postgresql.adba.pgdatatypes.Point;
import org.postgresql.adba.pgdatatypes.Polygon;

public class TextParser {
  private static final DateTimeFormatter timestampWithoutTimeZoneFormatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]");
  private static final DateTimeFormatter timestampWithTimeZoneFormatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]X");
  private static final DateTimeFormatter localDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter localTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss[.SSSSSS]");
  private static final DateTimeFormatter offsetTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss[.SSSSSS]X");

  private static final int IPV4_PART_COUNT = 4;
  private static final int IPV6_PART_COUNT = 8;

  public static Object boolOut(String in, Class<?> requestedClass) {
    return in.equals("t");
  }

  /**
   * reads a hex string into byte array.
   * @param in the hex string
   * @param requestedClass class requested by user
   * @return the bytes
   */
  public static Object byteaOut(String in, Class<?> requestedClass) {
    int len = in.length() - 2;
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(in.charAt(i + 2), 16) << 4)
          + Character.digit(in.charAt(i + 3), 16));
    }
    return data;
  }

  public static Object charOut(String in, Class<?> requestedClass) {
    return in.charAt(0);
  }

  public static Object nameout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  /**
   * Converts the string from the database to the requested class.
   * @param in the number as a string
   * @param requestedClass the class that the user wanted
   * @return a Number
   */
  public static Object int8Out(String in, Class<?> requestedClass) {
    if (Integer.class.equals(requestedClass)) {
      return Integer.parseInt(in);
    }

    if (Short.class.equals(requestedClass)) {
      return Short.parseShort(in);
    }

    return Long.parseLong(in);
  }

  /**
   * Converts the string from the database to the requested class.
   * @param in the number as a string
   * @param requestedClass the class that the user wanted
   * @return a Number
   */
  public static Object int2Out(String in, Class<?> requestedClass) {
    if (Long.class.equals(requestedClass)) {
      return Long.parseLong(in);
    }

    if (Integer.class.equals(requestedClass)) {
      return Integer.parseInt(in);
    }

    return Short.parseShort(in);
  }

  public static Object int2vectorout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  /**
   * Converts the string from the database to the requested class.
   * @param in the number as a string
   * @param requestedClass the class that the user wanted
   * @return a Number
   */
  public static Object int4Out(String in, Class<?> requestedClass) {
    if (Long.class.equals(requestedClass)) {
      return Long.parseLong(in);
    }

    if (Short.class.equals(requestedClass)) {
      return Short.parseShort(in);
    }

    return Integer.parseInt(in);
  }

  public static Object regprocout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  /**
   * Converts the string representing an oid from the database to the requested class.
   *
   * @param in the number as a string
   * @param requestedClass the class that the user wanted
   * @return a Long
   */
  public static Object oidOut(String in, Class<?> requestedClass) {
    return Long.parseLong(in);
  }

  /**
   * Converts the string representing and array of oid from the database to the requested class.
   *
   * @param in the oid array as a string, on the format {123,456}
   * @param requestedClass the class that the user wanted
   * @return a Long[]
   */
  public static Object oidOutArray(String in, Class<?> requestedClass) {
    if (in.equals("{}")) {
      return new Long[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    Long[] oids = new Long[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if (parts[i].equals("NULL")) {
        oids[i] = null;
      } else {
        oids[i] = Long.parseLong(parts[i]);
      }
    }
    return oids;
  }

  public static Object tidout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object xidout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object cidout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object oidvectorout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object pg_ddl_command_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object jsonOut(String in, Class<?> requestedClass) {
    return in;
  }

  public static Object pg_node_tree_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object smgrout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  /**
   * Converts a string representation a Point from the database to a Point object.
   *
   * @param in a string on the format (1.2,3.4)
   * @param requestedClass what the user wanted
   * @return a Point object
   */
  public static Object pointOut(String in, Class<?> requestedClass) {
    String[] parts = in.substring(1, in.length() - 1).split(",");

    return new Point(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
  }

  /**
   * Converts a string representation an array of Points from the database to a Point array in java.
   *
   * @param in a string on the format {"(1.2,3.4)",NULL}
   * @param requestedClass what the user wanted
   * @return a Point array
   */
  public static Object pointOutArray(String in, Class<?> requestedClass) {
    String[] parts = (String[]) textArrayOut(in, null);

    Point[] points = new Point[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        points[i] = null;
      } else {
        points[i] = (Point) pointOut(parts[i], Point.class);
      }
    }
    return points;
  }

  /**
   * Converts a string representation a LineSegment from the database to a LineSegment object.
   *
   * @param in a string on the format [(1.2,3.4),(1,2)]
   * @param requestedClass what the user wanted
   * @return a LineSegment object
   */
  public static Object lineSegmentOut(String in, Class<?> requestedClass) {
    String[] parts = in.substring(1, in.length() - 1).split(",");

    return new LineSegment(Double.parseDouble(parts[0].substring(1)), Double.parseDouble(parts[1]
        .substring(0, parts[1].length() - 1)), Double.parseDouble(parts[2].substring(1)), Double.parseDouble(parts[3]
        .substring(0, parts[3].length() - 1)));
  }

  /**
   * Converts a string representation an array of LineSegments from the database to a LineSegment array.
   *
   * @param in a string on the format ["(1.2,3.4),(1,2)",NULL]
   * @param requestedClass what the user wanted
   * @return a LineSegment array
   */
  public static Object lineSegmentOutArray(String in, Class<?> requestedClass) {
    String[] parts = (String[]) textArrayOut(in, null);

    LineSegment[] points = new LineSegment[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        points[i] = null;
      } else {
        points[i] = (LineSegment) lineSegmentOut(parts[i], LineSegment.class);
      }
    }
    return points;
  }

  /**
   * Converts a string representation a Path from the database to a Path object.
   *
   * @param in a string on the format ((x1,y1),...,(xn,yn))
   * @param requestedClass what the user wanted
   * @return a LineSegment object
   */
  public static Object pathOut(String in, Class<?> requestedClass) {
    String[] parts = in.substring(1, in.length() - 1).split(",");

    List<Point> points = new ArrayList<>();

    for (int i = 0; i < parts.length; i += 2) {
      points.add(new Point(Integer.parseInt(parts[i].substring(1)),
          Integer.parseInt(parts[i + 1].substring(0, parts[i + 1].length() - 1))));
    }

    return new Path(in.charAt(0) == '(', points);
  }

  /**
   * Converts a string representation an array of Paths from the database to a Path array.
   *
   * @param in a string on the format {"((1,2),(3,4))","((1,2),(3,4))"}
   * @param requestedClass what the user wanted
   * @return a Path array
   */
  public static Object pathOutArray(String in, Class<?> requestedClass) {
    String[] parts = (String[]) textArrayOut(in, null);

    Path[] paths = new Path[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        paths[i] = null;
      } else {
        paths[i] = (Path) pathOut(parts[i], Path.class);
      }
    }
    return paths;
  }

  /**
   * Converts a string representation a Box from the database to a Box object.
   *
   * @param in a string on the format (1.2,3.4),(1,2)
   * @param requestedClass what the user wanted
   * @return a Box object
   */
  public static Object boxOut(String in, Class<?> requestedClass) {
    String[] parts = in.split(",");

    return new Box(Double.parseDouble(parts[2].substring(1)), Double.parseDouble(parts[3]
        .substring(0, parts[3].length() - 1)), Double.parseDouble(parts[0].substring(1)), Double.parseDouble(parts[1]
        .substring(0, parts[1].length() - 1)));
  }

  /**
   * Converts a string representation an array of Boxes from the database to a Box array.
   *
   * @param in a string on the format ["(1.2,3.4),(1,2)";NULL]
   * @param requestedClass what the user wanted
   * @return a Box array
   */
  public static Object boxOutArray(String in, Class<?> requestedClass) {
    if (in.equals("{}")) {
      return new Box[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(";");

    Box[] points = new Box[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if (parts[i].equals("NULL")) {
        points[i] = null;
      } else {
        points[i] = (Box) boxOut(parts[i], Box.class);
      }
    }
    return points;
  }

  /**
   * Converts a string representation a Polygon from the database to a Polygon object.
   *
   * @param in a string on the format (1.2,3.4),(1,2)
   * @param requestedClass what the user wanted
   * @return a Polygon object
   */
  public static Object polyOut(String in, Class<?> requestedClass) {
    String[] parts = in.substring(1, in.length() - 1).split(",");

    List<Point> points = new ArrayList<>();

    for (int i = 0; i < parts.length; i += 2) {
      points.add(new Point(Integer.parseInt(parts[i].substring(1)),
          Integer.parseInt(parts[i + 1].substring(0, parts[i + 1].length() - 1))));
    }

    return new Polygon(points);
  }

  /**
   * Converts a string representation an array of Polygons from the database to a Polygon array.
   *
   * @param in a string on the format {"((1,2),(3,4))","((1,2),(3,4))"}
   * @param requestedClass what the user wanted
   * @return a Polygon array
   */
  public static Object polyOutArray(String in, Class<?> requestedClass) {
    String[] parts = (String[]) textArrayOut(in, null);

    Polygon[] polygons = new Polygon[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        polygons[i] = null;
      } else {
        polygons[i] = (Polygon) polyOut(parts[i], Path.class);
      }
    }
    return polygons;
  }

  /**
   * Converts a string representation a Line from the database to a Line object.
   *
   * @param in a string on the format {1.2,3.4,3}
   * @param requestedClass what the user wanted
   * @return a Line object
   */
  public static Object lineOut(String in, Class<?> requestedClass) {
    String[] parts = in.substring(1, in.length() - 1).split(",");

    return new Line(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
        Double.parseDouble(parts[2]));
  }

  /**
   * Converts a string representation an array of Lines from the database to a Line array in java.
   *
   * @param in a string on the format {"{1.2,3.4,2}",NULL}
   * @param requestedClass what the user wanted
   * @return a Line array
   */
  public static Object lineOutArray(String in, Class<?> requestedClass) {
    String[] parts = (String[]) textArrayOut(in, null);

    Line[] lines = new Line[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        lines[i] = null;
      } else {
        lines[i] = (Line) lineOut(parts[i], Line.class);
      }
    }
    return lines;
  }

  /**
   * Converts a CIDR from the database into an InetAddress object.
   *
   * @param in a string that represent an InetAddress
   * @param requestedClass what the user wanted
   * @return an InetAddress object
   */
  public static Object cidrOut(String in, Class<?> requestedClass) {
    in = in.split("/")[0]; // disregard network part

    // Make a first pass to categorize the characters in this string.
    boolean hasColon = false;
    boolean hasDot = false;
    for (int i = 0; i < in.length(); i++) {
      char c = in.charAt(i);
      if (c == '.') {
        hasDot = true;
      } else if (c == ':') {
        if (hasDot) {
          throw new SqlException("Received unparsable ip address from server", null, "", 0, "", 0);
        }
        hasColon = true;
      } else if (Character.digit(c, 16) == -1) {
        throw new SqlException("Received unparsable ip address from server", null, "", 0, "", 0);
      }
    }

    // Now decide which address family to parse.
    if (hasColon) {
      if (hasDot) {
        in = convertDottedQuadToHex(in);
        if (in == null) {
          throw new SqlException("Received unparsable ip address from server", null, "", 0, "", 0);
        }
      }
      try {
        byte[] result = textToNumericFormatV6(in);
        if (result == null) {
          throw new SqlException("Received unparsable ip address from server", null, "", 0, "", 0);
        }
        return InetAddress.getByAddress(result);
      } catch (UnknownHostException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    } else if (hasDot) {
      try {
        byte[] result = textToNumericFormatV4(in);
        if (result == null) {
          throw new SqlException("Received unparsable ip address from server", null, "", 0, "", 0);
        }
        return InetAddress.getByAddress(result);
      } catch (UnknownHostException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    throw new SqlException("Received unparsable ip address from server", null, "", 0, "", 0);
  }

  /**
   * Converts an array of CIDR from the database into an array of InetAddress objects.
   *
   * @param in a list of cidr elements in string form
   * @param requestedClass what the user wanted
   * @return an array of InetAddress objects
   */
  public static Object cidrOutArray(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new InetAddress[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    InetAddress[] result = new InetAddress[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = (InetAddress) cidrOut(parts[i], InetAddress.class);
      }
    }

    return result;
  }

  private static byte parseOctet(String ipPart) {
    // Note: we already verified that this string contains only hex digits.
    int octet = Integer.parseInt(ipPart);
    // Disallow leading zeroes, because no clear standard exists on
    // whether these should be interpreted as decimal or octal.
    if (octet > 255 || (ipPart.startsWith("0") && ipPart.length() > 1)) {
      throw new NumberFormatException();
    }
    return (byte) octet;
  }

  private static short parseHextet(String ipPart) {
    // Note: we already verified that this string contains only hex digits.
    int hextet = Integer.parseInt(ipPart, 16);
    if (hextet > 0xffff) {
      throw new NumberFormatException();
    }
    return (short) hextet;
  }

  private static byte [] textToNumericFormatV4(String ipString) {
    byte[] bytes = new byte[IPV4_PART_COUNT];
    int i = 0;
    try {
      for (String octet : ipString.split("\\.")) {
        bytes[i++] = parseOctet(octet);
      }
    } catch (NumberFormatException ex) {
      return null;
    }

    return i == IPV4_PART_COUNT ? bytes : null;
  }

  private static byte [] textToNumericFormatV6(String ipString) {
    // An address can have [2..8] colons, and N colons make N+1 parts.
    List<String> parts = Arrays.stream(ipString.split(":")).collect(Collectors.toList());
    if (parts.size() < 3 || parts.size() > IPV6_PART_COUNT + 1) {
      return null;
    }

    // Disregarding the endpoints, find "::" with nothing in between.
    // This indicates that a run of zeroes has been skipped.
    int skipIndex = -1;
    for (int i = 1; i < parts.size() - 1; i++) {
      if (parts.get(i).length() == 0) {
        if (skipIndex >= 0) {
          return null; // Can't have more than one ::
        }
        skipIndex = i;
      }
    }

    int partsHi; // Number of parts to copy from above/before the "::"
    int partsLo; // Number of parts to copy from below/after the "::"
    if (skipIndex >= 0) {
      // If we found a "::", then check if it also covers the endpoints.
      partsHi = skipIndex;
      partsLo = parts.size() - skipIndex - 1;
      if (parts.get(0).length() == 0 && --partsHi != 0) {
        return null; // ^: requires ^::
      }
      if (parts.get(parts.size() - 1).length() == 0 && --partsLo != 0) {
        return null; // :$ requires ::$
      }
    } else {
      // Otherwise, allocate the entire address to partsHi. The endpoints
      // could still be empty, but parseHextet() will check for that.
      partsHi = parts.size();
      partsLo = 0;
    }

    // If we found a ::, then we must have skipped at least one part.
    // Otherwise, we must have exactly the right number of parts.
    int partsSkipped = IPV6_PART_COUNT - (partsHi + partsLo);
    if (!(skipIndex >= 0 ? partsSkipped >= 1 : partsSkipped == 0)) {
      return null;
    }

    // Now parse the hextets into a byte array.
    ByteBuffer rawBytes = ByteBuffer.allocate(2 * IPV6_PART_COUNT);
    try {
      for (int i = 0; i < partsHi; i++) {
        rawBytes.putShort(parseHextet(parts.get(i)));
      }
      for (int i = 0; i < partsSkipped; i++) {
        rawBytes.putShort((short) 0);
      }
      for (int i = partsLo; i > 0; i--) {
        rawBytes.putShort(parseHextet(parts.get(parts.size() - i)));
      }
    } catch (NumberFormatException ex) {
      return null;
    }
    return rawBytes.array();
  }

  private static String convertDottedQuadToHex(String ipString) {
    int lastColon = ipString.lastIndexOf(':');
    String initialPart = ipString.substring(0, lastColon + 1);
    String dottedQuad = ipString.substring(lastColon + 1);
    byte[] quad = textToNumericFormatV4(dottedQuad);
    if (quad == null) {
      return null;
    }
    String penultimate = Integer.toHexString(((quad[0] & 0xff) << 8) | (quad[1] & 0xff));
    String ultimate = Integer.toHexString(((quad[2] & 0xff) << 8) | (quad[3] & 0xff));
    return initialPart + penultimate + ":" + ultimate;
  }

  public static Object float4Out(String in, Class<?> requestedClass) {
    return Float.parseFloat(in);
  }

  public static Object float8Out(String in, Class<?> requestedClass) {
    return Double.parseDouble(in);
  }

  public static Object abstimeout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object reltimeout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object tintervalout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  /**
   * If data is selected without a type being specified it ends up here.
   *
   * @param in data string
   * @param requestedClass what the user requested we parse the data as
   * @return our best effort or a RuntimeException is thrown
   */
  public static Object unknownOut(String in, Class<?> requestedClass) {
    if (String.class.equals(requestedClass)) {
      return in;
    }
    if (Boolean.class.equals(requestedClass) || boolean.class.equals(requestedClass)) {
      return Boolean.parseBoolean(in);
    }
    if (Integer.class.equals(requestedClass) || int.class.equals(requestedClass)) {
      return Integer.parseInt(in);
    }
    if (Short.class.equals(requestedClass) || short.class.equals(requestedClass)) {
      return Short.parseShort(in);
    }
    if (Byte.class.equals(requestedClass) || byte.class.equals(requestedClass)) {
      return Byte.parseByte(in);
    }
    if (Long.class.equals(requestedClass) || long.class.equals(requestedClass)) {
      return Long.parseLong(in);
    }
    if (Float.class.equals(requestedClass) || float.class.equals(requestedClass)) {
      return Float.parseFloat(in);
    }
    if (Double.class.equals(requestedClass) || double.class.equals(requestedClass)) {
      return Double.parseDouble(in);
    }

    throw new RuntimeException("value '" + in + "' can't be converted to " + requestedClass.getName());
  }

  /**
   * Converts a string representation a Circle from the database to a Circle object.
   *
   * @param in a string on the format &lt;(1,2),3&gt;
   * @param requestedClass what the user wanted
   * @return a Circle object
   */
  public static Object circleOut(String in, Class<?> requestedClass) {
    String[] parts = in.substring(1, in.length() - 1).split(",");

    return new Circle(Double.parseDouble(parts[0].substring(1)),
        Double.parseDouble(parts[1].substring(0, parts[1].length() - 1)), Double.parseDouble(parts[2]));
  }

  /**
   * Converts a string representation an array of Circles from the database to a Circle array in java.
   *
   * @param in a string on the format {"&lt;(1,2),3&gt;","&lt;(1,2),3&gt;"}
   * @param requestedClass what the user wanted
   * @return a Circle array
   */
  public static Object circleOutArray(String in, Class<?> requestedClass) {
    String[] parts = (String[]) textArrayOut(in, null);

    Circle[] circles = new Circle[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        circles[i] = null;
      } else {
        circles[i] = (Circle) circleOut(parts[i], Circle.class);
      }
    }
    return circles;
  }

  public static Object cash_out(String in, Class<?> requestedClass) {
    return in;
  }

  public static Object macaddr_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object inet_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object aclitemout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object bpCharOut(String in, Class<?> requestedClass) {
    return in.charAt(0);
  }

  /**
   * Converts the string from the database to an array of Character objects.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of Character objects
   */
  public static Object bpCharOutArray(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new Character[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    Character[] result = new Character[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = parts[i].charAt(0);
      }
    }

    return result;
  }

  public static Object varcharout(String in, Class<?> requestedClass) {
    return in;
  }

  public static Object dateOut(String in, Class<?> requestedClass) {
    return LocalDate.parse(in, localDateFormatter);
  }

  /**
   * Converts the string from the database to an array of LocalDate objects.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of LocalDate objects
   */
  public static Object dateOutArray(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new LocalDate[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    LocalDate[] result = new LocalDate[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = LocalDate.parse(parts[i], localDateFormatter);
      }
    }

    return result;
  }

  public static Object timeOut(String in, Class<?> requestedClass) {
    return LocalTime.parse(in, localTimeFormatter);
  }

  /**
   * Converts the string from the database to an array of LocalTime objects.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of LocalTime objects
   */
  public static Object timeOutArray(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new LocalTime[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    LocalTime[] result = new LocalTime[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = LocalTime.parse(parts[i], localTimeFormatter);
      }
    }

    return result;
  }

  /**
   * parses a timestamp into either a LocalDateTime or LocalTime based on what the user requested.
   * @param in string from the postgresql server
   * @param requestedClass class the user requested
   * @return object
   */
  public static Object timestampOut(String in, Class<?> requestedClass) {
    LocalDateTime ldt = LocalDateTime.parse(in, timestampWithoutTimeZoneFormatter);

    if (LocalTime.class.equals(requestedClass)) {
      return ldt.toLocalTime();
    }

    return ldt;
  }

  /**
   * Converts the string from the database to an array of LocalDateTime objects.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of LocalDateTime objects
   */
  public static Object timestampOutArray(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new LocalDateTime[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    LocalDateTime[] result = new LocalDateTime[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = LocalDateTime.parse(parts[i].substring(1, parts[i].length() - 1), timestampWithoutTimeZoneFormatter);
      }
    }

    return result;
  }

  public static Object timestampTimeZoneOut(String in, Class<?> requestedClass) {
    return OffsetDateTime.parse(in, timestampWithTimeZoneFormatter);
  }

  /**
   * Converts the string from the database to an array of LocalDateTime objects.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of LocalDateTime objects
   */
  public static Object timestampTimeZoneOutArray(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new OffsetDateTime[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    OffsetDateTime[] result = new OffsetDateTime[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = OffsetDateTime.parse(parts[i].substring(1, parts[i].length() - 1), timestampWithTimeZoneFormatter);
      }
    }

    return result;
  }

  /**
   * parses a string representation of an interval.
   * @param in the string to parse
   * @param requestedClass class requested by user
   * @return a Duration object
   */
  public static Object intervalOut(String in, Class<?> requestedClass) {
    final boolean IsoFormat = !in.startsWith("@");

    // Just a simple '0'
    if (!IsoFormat && in.length() == 3 && in.charAt(2) == '0') {
      return Duration.of(0, ChronoUnit.MICROS);
    }

    int years = 0;
    int months = 0;
    int days = 0;
    int hours = 0;
    int minutes = 0;
    double seconds = 0;

    try {
      String valueToken = null;

      in = in.replace('+', ' ').replace('@', ' ');
      final StringTokenizer st = new StringTokenizer(in);
      for (int i = 1; st.hasMoreTokens(); i++) {
        String token = st.nextToken();

        if ((i & 1) == 1) {
          int endHours = token.indexOf(':');
          if (endHours == -1) {
            valueToken = token;
            continue;
          }

          // This handles hours, minutes, seconds and microseconds for
          // ISO intervals
          int offset = (token.charAt(0) == '-') ? 1 : 0;

          hours = nullSafeIntGet(token.substring(offset, endHours));
          minutes = nullSafeIntGet(token.substring(endHours + 1, endHours + 3));

          // Pre 7.4 servers do not put second information into the results
          // unless it is non-zero.
          int endMinutes = token.indexOf(':', endHours + 1);
          if (endMinutes != -1) {
            seconds = nullSafeDoubleGet(token.substring(endMinutes + 1));
          }

          if (offset == 1) {
            hours = -hours;
            minutes = -minutes;
            seconds = -seconds;
          }

          valueToken = null;
        } else {
          // This handles years, months, days for both, ISO and
          // Non-ISO intervals. Hours, minutes, seconds and microseconds
          // are handled for Non-ISO intervals here.

          if (token.startsWith("year")) {
            years = nullSafeIntGet(valueToken);
          } else if (token.startsWith("mon")) {
            months = nullSafeIntGet(valueToken);
          } else if (token.startsWith("day")) {
            days = nullSafeIntGet(valueToken);
          } else if (token.startsWith("hour")) {
            hours = nullSafeIntGet(valueToken);
          } else if (token.startsWith("min")) {
            minutes = nullSafeIntGet(valueToken);
          } else if (token.startsWith("sec")) {
            seconds = nullSafeDoubleGet(valueToken);
          }
        }
      }
    } catch (NumberFormatException e) {
      throw new SqlException("Conversion of interval failed", e, "", 0, "", 0);
    }

    if (!IsoFormat && in.endsWith("ago")) {
      // Inverse the leading sign
      return Duration.of((long)((years * -31556952000000L) + (months * -2592000000000L) + (days * -86400000000L)
          + (hours * -3600000000L) + (minutes * -60000000L) + (seconds * -1000000L)), ChronoUnit.MICROS);
    } else {
      return Duration.of((long)((years * 31556952000000L) + (months * 2592000000000L) + (days * 86400000000L)
          + (hours * 3600000000L) + (minutes * 60000000L) + (seconds * 1000000L)), ChronoUnit.MICROS);
    }
  }

  /**
   * Parses an array of interval objects that come from the database.
   * @param in incoming data
   * @param requestedClass datatype that the user wanted
   * @return a Duration[]
   */
  public static Object intervalOutArray(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new Duration[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    return Arrays.stream(parts)
        .map(s -> {
          if (s.equals("NULL")) {
            return null;
          }
          return (Duration)intervalOut(s, Duration.class);
        })
        .collect(Collectors.toList()).toArray(new Duration[]{});
  }

  public static Object timetzOut(String in, Class<?> requestedClass) {
    return OffsetTime.parse(in, offsetTimeFormatter);
  }

  /**
   * Converts the string from the database to an array of OffsetTime objects.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of OffsetTime objects
   */
  public static Object timetzOutArray(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new OffsetTime[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    OffsetTime[] result = new OffsetTime[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = OffsetTime.parse(parts[i], offsetTimeFormatter);
      }
    }

    return result;
  }

  public static Object bitOut(String in, Class<?> requestedClass) {
    return parseBitString(in, requestedClass);
  }

  public static Object varBitOut(String in, Class<?> requestedClass) {
    return parseBitString(in, requestedClass);
  }

  private static Object parseBitString(String in, Class<?> requestedClass) {
    if (requestedClass == byte[].class) {
      return new BigInteger(in, 2).toByteArray();
    }

    if (in.length() == 1) {
      return Objects.equals("1", in);
    }

    return new BigInteger(in, 2).toByteArray();
  }

  /**
   * parse function for data that comes from an bit[] or varbit[] column.
   * @param in String with 0's and 1's
   * @param requestedClass what the user wanted
   * @return an byte[][] object.
   */
  public static Object bitOutArray(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new byte[][] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    byte[][] result = new byte[parts.length][];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = (byte[]) parseBitString(parts[i], byte[].class);
      }
    }

    return result;
  }

  public static Object numericOut(String in, Class<?> requestedClass) {
    return new BigDecimal(in);
  }

  /**
   * Converts the string from the database to an array of LocalDateTime objects.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of LocalDateTime objects
   */
  public static Object numericOutArray(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new BigDecimal[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    BigDecimal[] result = new BigDecimal[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = new BigDecimal(parts[i]);
      }
    }

    return result;
  }

  public static Object textOut(String in, Class<?> requestedClass) {
    return in;
  }

  public static Object regprocedureout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object regoperout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object regoperatorout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object regclassout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object regtypeout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object cstring_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object any_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object anyarray_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object void_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object trigger_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object language_handler_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object internal_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object opaque_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object anyelement_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object anynonarray_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object uuidOut(String in, Class<?> requestedClass) {
    return UUID.fromString(in);
  }

  /**
   * Converts the string from the database to an array of UUID objects.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of UUID objects
   */
  public static Object uuidOutArray(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new UUID[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    UUID[] result = new UUID[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = UUID.fromString(parts[i]);
      }
    }

    return result;
  }

  public static Object txid_snapshot_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object fdw_handler_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object pg_lsn_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object tsm_handler_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object anyenum_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object tsvectorout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object tsqueryout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object gtsvectorout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object regconfigout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object regdictionaryout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object jsonb_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object anyrange_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object event_trigger_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  /**
   * Converts the string from the database to a LongRange object.
   *
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return a LongRange object
   */
  public static Object integerRangeOut(String in, Class<?> requestedClass) {
    if ("empty".equals(in)) {
      return new IntegerRange();
    }

    boolean lowerInclusive = in.startsWith("[");
    boolean upperInclusive = in.endsWith("]");

    String[] parts = in.split(",");
    parts[0] = parts[0].substring(1);
    parts[1] = parts[1].substring(0, parts[1].length() - 1);

    return new IntegerRange(parts[0].length() == 0 ? null : Integer.parseInt(parts[0]),
        parts[1].length() == 0 ? null : Integer.parseInt(parts[1]), lowerInclusive, upperInclusive);
  }

  /**
   * Converts the string from the database to an array of LongRange objects.
   *
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of LongRange objects
   */
  public static Object integerRangeOutArray(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new LongRange[] {};
    }

    String[] parts = (String[]) textArrayOut(in, null);

    IntegerRange[] ranges = new IntegerRange[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        ranges[i] = null;
      } else {
        ranges[i] = (IntegerRange) integerRangeOut(parts[i], LongRange.class);
      }
    }
    return ranges;
  }

  /**
   * Converts the string from the database to a NumericRange object.
   *
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return a NumericRange object
   */
  public static Object numericRangeOut(String in, Class<?> requestedClass) {
    if ("empty".equals(in)) {
      return new NumericRange();
    }

    boolean lowerInclusive = in.startsWith("[");
    boolean upperInclusive = in.endsWith("]");

    String[] parts = in.split(",");
    parts[0] = parts[0].substring(1);
    parts[1] = parts[1].substring(0, parts[1].length() - 1);

    return new NumericRange(parts[0].length() == 0 ? null : new BigDecimal(parts[0]),
        parts[1].length() == 0 ? null : new BigDecimal(parts[1]), lowerInclusive, upperInclusive);
  }

  /**
   * Converts the string from the database to an array of NumericRange objects.
   *
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of NumericRange objects
   */
  public static Object numericRangeOutArray(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new NumericRange[] {};
    }

    String[] parts = (String[]) textArrayOut(in, null);

    NumericRange[] ranges = new NumericRange[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        ranges[i] = null;
      } else {
        ranges[i] = (NumericRange) numericRangeOut(parts[i], LongRange.class);
      }
    }
    return ranges;
  }

  /**
   * Converts the string from the database to a LocalDateTimeRange object.
   *
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return a LocalDateTimeRange object
   */
  public static Object localDateTimeRangeOut(String in, Class<?> requestedClass) {
    if ("empty".equals(in)) {
      return new LocalDateTimeRange();
    }

    boolean lowerInclusive = in.startsWith("[");
    boolean upperInclusive = in.endsWith("]");

    String[] parts = in.split(",");
    parts[0] = parts[0].substring(1);
    parts[1] = parts[1].substring(0, parts[1].length() - 1);

    return new LocalDateTimeRange(parts[0].length() == 0 ? null :
        LocalDateTime.parse(parts[0].substring(1, parts[0].length() - 1),
        timestampWithoutTimeZoneFormatter), parts[1].length() == 0 ? null :
        LocalDateTime.parse(parts[1].substring(1, parts[1].length() - 1),
        timestampWithoutTimeZoneFormatter), lowerInclusive, upperInclusive);
  }

  /**
   * Converts the string from the database to an array of LocalDateTimeRange objects.
   *
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of LocalDateTimeRange objects
   */
  public static Object localDateTimeRangeArrayOut(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new LocalDateTimeRange[] {};
    }

    String[] parts = (String[]) textArrayOut(in, null);

    LocalDateTimeRange[] ranges = new LocalDateTimeRange[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        ranges[i] = null;
      } else {
        ranges[i] = (LocalDateTimeRange) localDateTimeRangeOut(parts[i], LocalDateTimeRange.class);
      }
    }
    return ranges;
  }

  /**
   * Converts the string from the database to a OffsetDateTimeRange object.
   *
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return a OffsetDateTimeRange object
   */
  public static Object offsetDateTimeRangeOut(String in, Class<?> requestedClass) {
    if ("empty".equals(in)) {
      return new OffsetDateTimeRange();
    }

    boolean lowerInclusive = in.startsWith("[");
    boolean upperInclusive = in.endsWith("]");

    String[] parts = in.split(",");
    parts[0] = parts[0].substring(1);
    parts[1] = parts[1].substring(0, parts[1].length() - 1);

    return new OffsetDateTimeRange(parts[0].length() == 0 ? null :
        OffsetDateTime.parse(parts[0].substring(1, parts[0].length() - 1),
        timestampWithTimeZoneFormatter), parts[1].length() == 0 ? null :
        OffsetDateTime.parse(parts[1].substring(1, parts[1].length() - 1),
        timestampWithTimeZoneFormatter), lowerInclusive, upperInclusive);
  }

  /**
   * Converts the string from the database to an array of OffsetDateTimeRange objects.
   *
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of OffsetDateTimeRange objects
   */
  public static Object offsetDateTimeRangeArrayOut(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new OffsetDateTimeRange[] {};
    }

    String[] parts = (String[]) textArrayOut(in, null);

    OffsetDateTimeRange[] ranges = new OffsetDateTimeRange[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        ranges[i] = null;
      } else {
        ranges[i] = (OffsetDateTimeRange) offsetDateTimeRangeOut(parts[i], OffsetDateTimeRange.class);
      }
    }
    return ranges;
  }

  /**
   * Converts the string from the database to a LocalDateRange object.
   *
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return a LocalDateRange object
   */
  public static Object localDateRangeOut(String in, Class<?> requestedClass) {
    if ("empty".equals(in)) {
      return new LocalDateRange();
    }

    boolean lowerInclusive = in.startsWith("[");
    boolean upperInclusive = in.endsWith("]");

    String[] parts = in.split(",");
    parts[0] = parts[0].substring(1);
    parts[1] = parts[1].substring(0, parts[1].length() - 1);

    return new LocalDateRange(parts[0].length() == 0 ? null :
        LocalDate.parse(parts[0],
            localDateFormatter), parts[1].length() == 0 ? null :
        LocalDate.parse(parts[1],
            localDateFormatter), lowerInclusive, upperInclusive);
  }

  /**
   * Converts the string from the database to an array of LocalDateRange objects.
   *
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of LocalDateRange objects
   */
  public static Object localDateRangeArrayOut(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new LocalDateRange[] {};
    }

    String[] parts = (String[]) textArrayOut(in, null);

    LocalDateRange[] ranges = new LocalDateRange[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        ranges[i] = null;
      } else {
        ranges[i] = (LocalDateRange) localDateRangeOut(parts[i], LocalDateRange.class);
      }
    }
    return ranges;
  }

  /**
   * Converts the string from the database to a LongRange object.
   *
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return a LongRange object
   */
  public static Object longRangeOut(String in, Class<?> requestedClass) {
    if ("empty".equals(in)) {
      return new LongRange();
    }

    boolean lowerInclusive = in.startsWith("[");
    boolean upperInclusive = in.endsWith("]");

    String[] parts = in.split(",");
    parts[0] = parts[0].substring(1);
    parts[1] = parts[1].substring(0, parts[1].length() - 1);

    return new LongRange(parts[0].length() == 0 ? null : Long.parseLong(parts[0]),
        parts[1].length() == 0 ? null : Long.parseLong(parts[1]), lowerInclusive, upperInclusive);
  }

  /**
   * Converts the string from the database to an array of LongRange objects.
   *
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of LongRange objects
   */
  public static Object longRangeOutArray(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new LongRange[] {};
    }

    String[] parts = (String[]) textArrayOut(in, null);

    LongRange[] ranges = new LongRange[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        ranges[i] = null;
      } else {
        ranges[i] = (LongRange) longRangeOut(parts[i], LongRange.class);
      }
    }
    return ranges;
  }

  public static Object range_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object regnamespaceout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object regroleout(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object array_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  /**
   * Converts the string from the database to an array of shorts.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of shorts
   */
  public static Object int2ArrayOut(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new Short[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    Short[] result = new Short[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = Short.parseShort(parts[i]);
      }
    }

    return result;
  }

  /**
   * Converts the string from the database to an array of ints.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of ints
   */
  public static Object int4ArrayOut(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new Integer[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    Integer[] result = new Integer[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = Integer.parseInt(parts[i]);
      }
    }

    return result;
  }

  /**
   * Converts the string from the database to an array of longs.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of longs
   */
  public static Object int8ArrayOut(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new Long[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    Long[] result = new Long[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = Long.parseLong(parts[i]);
      }
    }

    return result;
  }

  /**
   * Converts the string from the database to an array of floats.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of floats
   */
  public static Object floatArrayOut(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new Float[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    Float[] result = new Float[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = Float.parseFloat(parts[i]);
      }
    }

    return result;
  }

  /**
   * Converts the string from the database to an array of doubles.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of doubles
   */
  public static Object doubleArrayOut(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new Double[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    Double[] result = new Double[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = Double.parseDouble(parts[i]);
      }
    }

    return result;
  }

  /**
   * Converts the string from the database to an array of doubles.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of doubles
   */
  public static Object booleanArrayOut(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new Boolean[] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    Boolean[] result = new Boolean[parts.length];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = "t".equals(parts[i]);
      }
    }

    return result;
  }

  /**
   * Converts the string from the database to an array of doubles.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of doubles
   */
  public static Object byteaArrayOut(String in, Class<?> requestedClass) {
    if ("{}".equals(in)) {
      return new byte[][] {};
    }

    String[] parts = in.substring(1, in.length() - 1).split(",");

    byte[][] result = new byte[parts.length][];

    for (int i = 0; i < parts.length; i++) {
      if ("NULL".equals(parts[i])) {
        result[i] = null;
      } else {
        result[i] = new byte[(parts[i].length() - 4) / 2];
        for (int j = 0; j < parts[i].length() - 6; j += 2) {
          result[i][j / 2] = (byte) ((Character.digit(parts[i].charAt(j + 4), 16) << 4)
              + Character.digit(parts[i].charAt(j + 5), 16));
        }
      }
    }

    return result;
  }

  /**
   * Converts the string from the database to an array of strings.
   * @param in the array as a string
   * @param requestedClass the class that the user wanted
   * @return an array of strings
   */
  public static Object textArrayOut(String in, Class<?> requestedClass) {
    List<String> result = new ArrayList<>();

    StringBuilder sb = new StringBuilder();
    boolean stringStarted = false;
    boolean insideQuotes = false;
    boolean escapeNext = false;
    for (int i = 0; i < in.length(); i++) {
      if (!stringStarted) {
        if (in.charAt(i) == '{' && in.charAt(i + 1) != '"') {
          stringStarted = true;
        } else if (in.charAt(i) == '}' && in.charAt(i - 1) == ',') {
          result.add(sb.toString());
          sb.setLength(0);
        } else if (in.charAt(i) == '{' && in.charAt(i + 1) == '"') {
          continue;
        } else if (in.charAt(i - 1) == ',' && in.charAt(i) == ',') {
          result.add("");
          stringStarted = true;
        } else if (in.charAt(i - 1) == ',' && in.charAt(i) != '"') {
          if (in.charAt(i) != ',') {
            sb.append(in.charAt(i));
          }
          stringStarted = true;
        } else if (in.charAt(i) == '"') {
          stringStarted = true;
          insideQuotes = true;
        }
      } else {
        if (escapeNext) {
          sb.append(in.charAt(i));
          escapeNext = false;
        } else if (in.charAt(i) == '\\') {
          escapeNext = true;
        } else if (in.charAt(i) == '"' && insideQuotes) {
          result.add(sb.toString());
          sb.setLength(0);
          insideQuotes = false;
          stringStarted = false;
        } else if (in.charAt(i) == ',' && !insideQuotes) {
          String str = sb.toString();
          if ("NULL".equals(str)) {
            result.add(null);
          } else {
            result.add(str);
          }
          sb.setLength(0);
          stringStarted = false;
        } else if (in.charAt(i) == '}' && !insideQuotes && in.charAt(i - 1) == '{') {
          stringStarted = false;
        } else if (in.charAt(i) == '}' && !insideQuotes) {
          String str = sb.toString();
          if ("NULL".equals(str)) {
            result.add(null);
          } else {
            result.add(str);
          }
          sb.setLength(0);
          stringStarted = false;
        } else {
          sb.append(in.charAt(i));
        }
      }
    }

    return result.toArray(new String[]{});
  }

  public static Object record_out(String in, Class<?> requestedClass) {
    throw new RuntimeException("not implemented yet");
  }

  public static Object passthrough(String in, Class<?> requestedClass) {
    return in;
  }

  /**
   * Returns integer value of value or 0 if value is null.
   *
   * @param value integer as string value
   * @return integer parsed from string value
   * @throws NumberFormatException if the string contains invalid chars
   */
  private static int nullSafeIntGet(String value) throws NumberFormatException {
    return (value == null) ? 0 : Integer.parseInt(value);
  }

  /**
   * Returns double value of value or 0 if value is null.
   *
   * @param value double as string value
   * @return double parsed from string value
   * @throws NumberFormatException if the string contains invalid chars
   */
  private static double nullSafeDoubleGet(String value) throws NumberFormatException {
    return (value == null) ? 0 : Double.parseDouble(value);
  }
}
