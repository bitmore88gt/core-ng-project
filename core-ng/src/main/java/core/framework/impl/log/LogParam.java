package core.framework.impl.log;

import core.framework.api.util.Charsets;

import java.util.Map;

/**
 * @author neo
 */
public class LogParam {  // used to hold bytes in log message, only eval on flush, to save memory
    public static Object of(byte[] bytes) {
        return new StringParam(bytes);
    }

    public static Object of(Map<?, ?> map) {
        return new MapParam(map);
    }

    private static class StringParam {
        private final byte[] bytes;

        StringParam(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public String toString() {
            if (bytes == null) return null;
            return new String(bytes, Charsets.UTF_8);
        }
    }

    private static class MapParam {
        private final Map<?, ?> map;

        MapParam(Map<?, ?> map) {
            this.map = map;
        }

        // replicate AbstractMap.toString with encoding
        @Override
        public String toString() {
            if (map == null) return null;
            StringBuilder builder = new StringBuilder();
            int index = 0;
            builder.append('{');
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (index > 0) builder.append(", ");
                Object key = entry.getKey();
                Object value = entry.getValue();
                builder.append(encode(key));
                builder.append('=');
                builder.append(encode(value));
                index++;
            }
            return builder.append('}').toString();
        }

        private String encode(Object value) {
            if (value instanceof byte[]) return new String((byte[]) value, Charsets.UTF_8);
            return String.valueOf(value);
        }
    }
}
