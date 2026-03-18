package org.example.back.common.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;

import java.util.Date;

/**
 * 简单编码生成器
 */
public final class CodeGenerator {

    private CodeGenerator() {
    }

    public static String deptCode() {
        return "DEPT" + timestampSuffix();
    }

    public static String employeeCode() {
        return "EMP" + timestampSuffix();
    }

    public static String supplierCode() {
        return "SUP" + timestampSuffix();
    }

    public static String goodsCode() {
        return "GD" + timestampSuffix();
    }

    public static String purchaseNo() {
        return "PUR" + timestampSuffix();
    }

    public static String purchaseReturnNo() {
        return "PRET" + timestampSuffix();
    }

    public static String salesNo() {
        return "SAL" + timestampSuffix();
    }

    public static String salesReturnNo() {
        return "SRET" + timestampSuffix();
    }

    private static String timestampSuffix() {
        return DateUtil.format(new Date(), "yyMMddHHmmss") + RandomUtil.randomNumbers(3);
    }
}