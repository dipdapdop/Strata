/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyPair;

/**
 * Test {@link FxForwardSensitivity}.
 */
@Test
public class FxForwardSensitivityTest {

  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, GBP);
  private static final LocalDate REFERENCE_DATE = LocalDate.of(2015, 11, 23);
  private static final double SENSITIVITY = 1.34d;

  public void test_of_withoutCurrency() {
    FxForwardSensitivity test = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getReferenceCounterCurrency(), EUR);
    assertEquals(test.getReferenceCurrency(), GBP);
    assertEquals(test.getReferenceDate(), REFERENCE_DATE);
    assertEquals(test.getSensitivity(), SENSITIVITY);
  }

  public void test_of_withCurrency() {
    FxForwardSensitivity test = FxForwardSensitivity.of(CURRENCY_PAIR, USD, EUR, REFERENCE_DATE, SENSITIVITY);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getReferenceCounterCurrency(), GBP);
    assertEquals(test.getReferenceCurrency(), EUR);
    assertEquals(test.getReferenceDate(), REFERENCE_DATE);
    assertEquals(test.getSensitivity(), SENSITIVITY);
  }

  public void test_of_wrongRefCurrency() {
    assertThrowsIllegalArg(() -> FxForwardSensitivity.of(CURRENCY_PAIR, USD, REFERENCE_DATE, SENSITIVITY));
    assertThrowsIllegalArg(() -> FxForwardSensitivity.of(CURRENCY_PAIR, USD, USD, REFERENCE_DATE, SENSITIVITY));
  }

  //-------------------------------------------------------------------------
  public void test_withCurrency_same() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.withCurrency(EUR);
    assertEquals(test, base);
  }

  public void test_withCurrency_other() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.withCurrency(USD);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getReferenceCounterCurrency(), EUR);
    assertEquals(test.getReferenceCurrency(), GBP);
    assertEquals(test.getReferenceDate(), REFERENCE_DATE);
    assertEquals(test.getSensitivity(), SENSITIVITY);
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.withSensitivity(13.5d);
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getReferenceCounterCurrency(), EUR);
    assertEquals(test.getReferenceCurrency(), GBP);
    assertEquals(test.getReferenceDate(), REFERENCE_DATE);
    assertEquals(test.getSensitivity(), 13.5d);
  }

  //-------------------------------------------------------------------------
  public void test_compareExcludingSensitivity() {
    FxForwardSensitivity a1 = FxForwardSensitivity.of(CURRENCY_PAIR, EUR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity a2 = FxForwardSensitivity.of(CURRENCY_PAIR, EUR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity b = FxForwardSensitivity.of(CurrencyPair.of(GBP, USD), EUR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity c = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, EUR, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity d = FxForwardSensitivity.of(CURRENCY_PAIR, JPY, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity e = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, date(2015, 9, 27), SENSITIVITY);
    ZeroRateSensitivity other = ZeroRateSensitivity.of(GBP, date(2015, 9, 27), SENSITIVITY);
    assertEquals(a1.compareExcludingSensitivity(a2), 0);
    assertEquals(a1.compareExcludingSensitivity(b) < 0, true);
    assertEquals(b.compareExcludingSensitivity(a1) > 0, true);
    assertEquals(a1.compareExcludingSensitivity(c) < 0, true);
    assertEquals(c.compareExcludingSensitivity(a1) > 0, true);
    assertEquals(a1.compareExcludingSensitivity(d) < 0, true);
    assertEquals(d.compareExcludingSensitivity(a1) > 0, true);
    assertEquals(a1.compareExcludingSensitivity(e) > 0, true);
    assertEquals(e.compareExcludingSensitivity(a1) < 0, true);
    assertEquals(a1.compareExcludingSensitivity(other) < 0, true);
    assertEquals(other.compareExcludingSensitivity(a1) > 0, true);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.multipliedBy(2.4d);
    FxForwardSensitivity expected = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY * 2.4d);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_mapSensitivity() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.mapSensitivity(s -> 1d / s);
    FxForwardSensitivity expected = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, 1d / SENSITIVITY);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_normalize() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.normalize();
    assertEquals(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    FxForwardSensitivity base1 = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity base2 = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, 1.56d);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base1).add(base2);
    PointSensitivityBuilder test = base1.combinedWith(base2);
    assertEquals(test, expected);
  }

  public void test_combinedWith_mutable() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    MutablePointSensitivities expected = new MutablePointSensitivities();
    expected.add(base);
    PointSensitivityBuilder test = base.combinedWith(new MutablePointSensitivities());
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_buildInto() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    MutablePointSensitivities combo = new MutablePointSensitivities();
    MutablePointSensitivities test = base.buildInto(combo);
    assertSame(test, combo);
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_build() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    PointSensitivities test = base.build();
    assertEquals(test.getSensitivities(), ImmutableList.of(base));
  }

  //-------------------------------------------------------------------------
  public void test_cloned() {
    FxForwardSensitivity base = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    FxForwardSensitivity test = base.cloned();
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxForwardSensitivity test1 = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    coverImmutableBean(test1);
    FxForwardSensitivity test2 = FxForwardSensitivity.of(CurrencyPair.of(USD, JPY), JPY, date(2015, 9, 27), 4.25d);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxForwardSensitivity test = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, REFERENCE_DATE, SENSITIVITY);
    assertSerialization(test);
  }

}
