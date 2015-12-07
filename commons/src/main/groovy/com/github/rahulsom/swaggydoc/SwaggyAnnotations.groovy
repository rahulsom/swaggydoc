package com.github.rahulsom.swaggydoc

import com.wordnik.swagger.annotations.ApiImplicitParam

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggyList {
    /**
     * Whether to define a param q for search. Defaults true
     * @return
     */
    boolean searchParam() default true
    ApiImplicitParam[] extraParams() default []
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggyShow {
    ApiImplicitParam[] extraParams() default []
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggySave {
    ApiImplicitParam[] extraParams() default []
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggyUpdate {
    ApiImplicitParam[] extraParams() default []
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggyPatch {
    ApiImplicitParam[] extraParams() default []
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggyDelete {
    ApiImplicitParam[] extraParams() default []
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggyAdditionalClasses {
    Class[] value()
}

