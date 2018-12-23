package opencontacts.open.com.opencontacts.utils;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by sultanm on 7/30/17.
 */

public class Common {
    public static final Pattern NON_ASCII_REGEX_MATCHER = Pattern.compile("[^\\p{ASCII}]");

    public static String replaceAccentedCharactersWithEnglish(String string) {
        String normalizedString = Normalizer.normalize(string, Normalizer.Form.NFD);
        return NON_ASCII_REGEX_MATCHER.matcher(normalizedString).replaceAll("");
    }

    public static String getDurationInMinsAndSecs(int duration){
        NumberFormat twoDigitFormat = NumberFormat.getInstance();
        twoDigitFormat.setMinimumIntegerDigits(2);
        return twoDigitFormat.format(duration / 60) + ":" + twoDigitFormat.format(duration % 60);
    }

    public static <K, V> V getOrDefault(Map<K, V> map, K key, V defaultValue){
        V v = map.get(key);
        return (v == null)? defaultValue : v;
    }

    public static <T> List<T> times(int count, TimesFunction<T> timesFunction){
        ArrayList<T> list = new ArrayList<>(count);
        for(int i=0; i<count; i++){
            list.add(timesFunction.apply(i));
        }
        return list;
    }

    public interface TimesFunction<T>{
        T apply(int count);
    }

}
