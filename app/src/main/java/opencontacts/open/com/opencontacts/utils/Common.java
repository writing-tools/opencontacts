package opencontacts.open.com.opencontacts.utils;

import com.github.underscore.Function;
import com.github.underscore.U;

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

    public static <T> List<T> mapIndexes(int count, TimesFunction<T> timesFunction){
        ArrayList<T> list = new ArrayList<>(count);
        for(int i=0; i < count; i++){
            list.add(timesFunction.apply(i));
        }
        return list;
    }

    public static void forEachIndex(int count, ForEachIndexFunction function){
        for(int i=0; i < count; i++){
            function.apply(i);
        }
    }

    public static boolean forEachIndexUntilFalseElseEndWithTrue(int count, ForEachIndexUntilFalseFunction function){
        for(int i=0; i < count; i++){
            if(!function.apply(i))
                return false;
        }
        return true;
    }

    public static <T> int findIndexOrDefault(List<T> listOfItems, T item, int defaultIndex) {
        int indexOfItem = listOfItems.indexOf(item);
        return indexOfItem == -1 ? defaultIndex : indexOfItem;
    }

    public static <J> List<List> mapMultiple(List<J> items, Function<J, ?>... functions) {
        int size = items.size();
        List<List> finalList = mapIndexes(functions.length, index -> new ArrayList<>(size));
        U.forEachIndexed(items, (index, item) -> finalList.get(index).add(functions[index].apply(item)));
        return finalList;
    }

    public static <F, T> List<T> map(Iterable<F> iterable, Function<F, T> function){
        ArrayList<T> list = new ArrayList<>(0);
        for (F item : iterable) {
            list.add(function.apply(item));
        }
        return list;
    }

    public interface TimesFunction<T>{
        T apply(int index);
    }

    public interface ForEachIndexFunction{
        void apply(int index);
    }

    public interface ForEachIndexUntilFalseFunction {
        boolean apply(int index);
    }

}
