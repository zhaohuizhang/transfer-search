package com.bank.transfersearch.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PinyinUtilTest {

    @Test
    void testGetPinyin_ChineseOnly() {
        assertEquals("zhangsan", PinyinUtil.getPinyin("张三"));
        assertEquals("zhangwei", PinyinUtil.getPinyin("张伟"));
    }

    @Test
    void testGetPinyin_Mixed() {
        assertEquals("zhzhang", PinyinUtil.getPinyin("zh张"));
        assertEquals("xyz", PinyinUtil.getPinyin("xyz"));
    }

    @Test
    void testGetInitial_ChineseOnly() {
        assertEquals("zs", PinyinUtil.getInitial("张三"));
        assertEquals("zw", PinyinUtil.getInitial("张伟"));
    }

    @Test
    void testGetInitial_Mixed() {
        assertEquals("zhz", PinyinUtil.getInitial("zh张"));
    }
    
    @Test
    void testNullOrEmpty() {
        assertEquals("", PinyinUtil.getPinyin(null));
        assertEquals("", PinyinUtil.getPinyin(""));
        assertEquals("", PinyinUtil.getInitial(null));
        assertEquals("", PinyinUtil.getInitial(""));
    }
}
