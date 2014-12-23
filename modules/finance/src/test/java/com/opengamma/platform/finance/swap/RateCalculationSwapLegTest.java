/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import static com.opengamma.basics.PayReceive.PAY;
import static com.opengamma.basics.PayReceive.RECEIVE;
import static com.opengamma.basics.currency.Currency.EUR;
import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.basics.date.DayCounts.ACT_360;
import static com.opengamma.basics.date.DayCounts.ACT_365F;
import static com.opengamma.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.basics.index.FxIndices.ECB_EUR_GBP;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.basics.schedule.Frequency.P1M;
import static com.opengamma.basics.schedule.Frequency.P2M;
import static com.opengamma.basics.schedule.Frequency.P3M;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static com.opengamma.platform.finance.swap.CompoundingMethod.STRAIGHT;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.date.BusinessDayAdjustment;
import com.opengamma.basics.date.DayCounts;
import com.opengamma.basics.date.DaysAdjustment;
import com.opengamma.basics.schedule.PeriodicSchedule;
import com.opengamma.basics.value.ValueAdjustment;
import com.opengamma.basics.value.ValueSchedule;
import com.opengamma.basics.value.ValueStep;
import com.opengamma.platform.finance.rate.FixedRate;
import com.opengamma.platform.finance.rate.IborRate;

/**
 * Test.
 */
@Test
public class RateCalculationSwapLegTest {

  private static final LocalDate DATE_01_02 = date(2014, 1, 2);
  private static final LocalDate DATE_01_05 = date(2014, 1, 5);
  private static final LocalDate DATE_01_06 = date(2014, 1, 6);
  private static final LocalDate DATE_02_03 = date(2014, 2, 3);
  private static final LocalDate DATE_02_05 = date(2014, 2, 5);
  private static final LocalDate DATE_02_07 = date(2014, 2, 7);
  private static final LocalDate DATE_03_03 = date(2014, 3, 3);
  private static final LocalDate DATE_03_05 = date(2014, 3, 5);
  private static final LocalDate DATE_03_07 = date(2014, 3, 7);
  private static final LocalDate DATE_04_03 = date(2014, 4, 3);
  private static final LocalDate DATE_04_05 = date(2014, 4, 5);
  private static final LocalDate DATE_04_07 = date(2014, 4, 7);
  private static final LocalDate DATE_04_09 = date(2014, 4, 9);
  private static final LocalDate DATE_05_01 = date(2014, 5, 1);
  private static final LocalDate DATE_05_05 = date(2014, 5, 5);
  private static final LocalDate DATE_05_06 = date(2014, 5, 6);
  private static final LocalDate DATE_05_08 = date(2014, 5, 8);
  private static final LocalDate DATE_06_05 = date(2014, 6, 5);
  private static final LocalDate DATE_06_09 = date(2014, 6, 9);
  private static final DaysAdjustment PLUS_THREE_DAYS = DaysAdjustment.ofBusinessDays(3, GBLO);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);
  private static final DaysAdjustment MINUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(-2, GBLO);

  //-------------------------------------------------------------------------
  public void test_builder() {
    PeriodicSchedule accrualSchedule = PeriodicSchedule.builder()
        .startDate(DATE_01_05)
        .endDate(DATE_04_05)
        .frequency(P1M)
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
        .build();
    PaymentSchedule paymentSchedule = PaymentSchedule.builder()
        .paymentFrequency(P1M)
        .paymentOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    FixedRateCalculation rateCalc = FixedRateCalculation.builder()
        .dayCount(DayCounts.ACT_365F)
        .rate(ValueSchedule.of(0.025d))
        .build();
    NotionalSchedule notionalSchedule = NotionalSchedule.of(GBP, 1000d);
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(accrualSchedule)
        .paymentSchedule(paymentSchedule)
        .notionalSchedule(notionalSchedule)
        .calculation(rateCalc)
        .build();
    assertEquals(test.getStartDate(), DATE_01_06);
    assertEquals(test.getEndDate(), DATE_04_07);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getPayReceive(), PAY);
    assertEquals(test.getAccrualSchedule(), accrualSchedule);
    assertEquals(test.getPaymentSchedule(), paymentSchedule);
    assertEquals(test.getNotionalSchedule(), notionalSchedule);
    assertEquals(test.getCalculation(), rateCalc);
  }

  //-------------------------------------------------------------------------
  public void test_expand_oneAccrualPerPayment_fixedRate() {
    // test case
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_04_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentOffset(PLUS_TWO_DAYS)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, 1000d))
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_365F)
            .rate(ValueSchedule.of(0.025d))
            .build())
        .build();
    // expected
    RatePaymentPeriod rpp1 = RatePaymentPeriod.builder()
        .paymentDate(DATE_02_07)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_01_06)
            .endDate(DATE_02_05)
            .unadjustedStartDate(DATE_01_05)
            .yearFraction(ACT_365F.yearFraction(DATE_01_06, DATE_02_05))
            .rate(FixedRate.of(0.025d))
            .build())
        .currency(GBP)
        .notional(-1000d)
        .build();
    RatePaymentPeriod rpp2 = RatePaymentPeriod.builder()
        .paymentDate(DATE_03_07)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_02_05)
            .endDate(DATE_03_05)
            .yearFraction(ACT_365F.yearFraction(DATE_02_05, DATE_03_05))
            .rate(FixedRate.of(0.025d))
            .build())
        .currency(GBP)
        .notional(-1000d)
        .build();
    RatePaymentPeriod rpp3 = RatePaymentPeriod.builder()
        .paymentDate(DATE_04_09)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_03_05)
            .endDate(DATE_04_07)
            .unadjustedEndDate(DATE_04_05)
            .yearFraction(ACT_365F.yearFraction(DATE_03_05, DATE_04_07))
            .rate(FixedRate.of(0.025d))
            .build())
        .currency(GBP)
        .notional(-1000d)
        .build();
    // assertion
    assertEquals(test.expand(), ExpandedSwapLeg.builder()
        .paymentPeriods(rpp1, rpp2, rpp3)
        .build());
  }

  public void test_expand_twoAccrualsPerPayment_iborRate_varyingNotional_notionalExchange() {
    // test case
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_06_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P2M)
            .paymentOffset(PLUS_TWO_DAYS)
            .compoundingMethod(STRAIGHT)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .currency(GBP)
            .amount(ValueSchedule.of(1000d, ValueStep.of(1, ValueAdjustment.ofAbsoluteAmount(1500d))))
            .initialExchange(true)
            .intermediateExchange(true)
            .finalExchange(true)
            .build())
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_365F)
            .index(GBP_LIBOR_1M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
            .build())
        .build();
    // expected
    RatePaymentPeriod rpp1 = RatePaymentPeriod.builder()
        .paymentDate(DATE_03_07)
        .accrualPeriods(
            RateAccrualPeriod.builder()
                .startDate(DATE_01_06)
                .endDate(DATE_02_05)
                .unadjustedStartDate(DATE_01_05)
                .yearFraction(ACT_365F.yearFraction(DATE_01_06, DATE_02_05))
                .rate(IborRate.of(GBP_LIBOR_1M, DATE_01_02))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_02_05)
                .endDate(DATE_03_05)
                .yearFraction(ACT_365F.yearFraction(DATE_02_05, DATE_03_05))
                .rate(IborRate.of(GBP_LIBOR_1M, DATE_02_03))
                .build())
        .currency(GBP)
        .notional(-1000d)
        .compoundingMethod(STRAIGHT)
        .build();
    RatePaymentPeriod rpp2 = RatePaymentPeriod.builder()
        .paymentDate(DATE_05_08)
        .accrualPeriods(
            RateAccrualPeriod.builder()
                .startDate(DATE_03_05)
                .endDate(DATE_04_07)
                .unadjustedEndDate(DATE_04_05)
                .yearFraction(ACT_365F.yearFraction(DATE_03_05, DATE_04_07))
                .rate(IborRate.of(GBP_LIBOR_1M, DATE_03_03))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_04_07)
                .endDate(DATE_05_06)
                .unadjustedStartDate(DATE_04_05)
                .unadjustedEndDate(DATE_05_05)
                .yearFraction(ACT_365F.yearFraction(DATE_04_07, DATE_05_06))
                .rate(IborRate.of(GBP_LIBOR_1M, DATE_04_03))
                .build())
        .currency(GBP)
        .notional(-1500d)
        .compoundingMethod(STRAIGHT)
        .build();
    RatePaymentPeriod rpp3 = RatePaymentPeriod.builder()
        .paymentDate(DATE_06_09)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_05_06)
            .endDate(DATE_06_05)
            .unadjustedStartDate(DATE_05_05)
            .yearFraction(ACT_365F.yearFraction(DATE_05_06, DATE_06_05))
            .rate(IborRate.of(GBP_LIBOR_1M, DATE_05_01))
            .build())
        .currency(GBP)
        .notional(-1500d)
        .compoundingMethod(STRAIGHT)
        .build();
    // events (only one intermediate exchange)
    NotionalExchange nexInitial = NotionalExchange.of(DATE_01_06, CurrencyAmount.of(GBP, 1000d));
    NotionalExchange nexIntermediate = NotionalExchange.of(DATE_03_07, CurrencyAmount.of(GBP, 500d));
    NotionalExchange nexFinal = NotionalExchange.of(DATE_06_09, CurrencyAmount.of(GBP, -1500d));
    // assertion
    assertEquals(test.expand(), ExpandedSwapLeg.builder()
        .paymentPeriods(rpp1, rpp2, rpp3)
        .paymentEvents(nexInitial, nexIntermediate, nexFinal)
        .build());
  }

  public void test_expand_threeAccrualsPerPayment() {
    // test case
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_04_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentOffset(PLUS_TWO_DAYS)
            .compoundingMethod(STRAIGHT)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, 1000d))
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_365F)
            .rate(ValueSchedule.of(0.025d))
            .build())
        .build();
    // expected
    RatePaymentPeriod rpp1 = RatePaymentPeriod.builder()
        .paymentDate(DATE_04_09)
        .accrualPeriods(
            RateAccrualPeriod.builder()
                .startDate(DATE_01_06)
                .endDate(DATE_02_05)
                .unadjustedStartDate(DATE_01_05)
                .yearFraction(ACT_365F.yearFraction(DATE_01_06, DATE_02_05))
                .rate(FixedRate.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_02_05)
                .endDate(DATE_03_05)
                .yearFraction(ACT_365F.yearFraction(DATE_02_05, DATE_03_05))
                .rate(FixedRate.of(0.025d))
                .build(),
            RateAccrualPeriod.builder()
                .startDate(DATE_03_05)
                .endDate(DATE_04_07)
                .unadjustedEndDate(DATE_04_05)
                .yearFraction(ACT_365F.yearFraction(DATE_03_05, DATE_04_07))
                .rate(FixedRate.of(0.025d))
                .build())
        .currency(GBP)
        .notional(-1000d)
        .compoundingMethod(STRAIGHT)
        .build();
    // assertion
    assertEquals(test.expand(), ExpandedSwapLeg.builder()
        .paymentPeriods(rpp1)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_expand_oneAccrualPerPayment_fxReset() {
    // test case
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_04_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentOffset(PLUS_TWO_DAYS)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .currency(GBP)
            .amount(ValueSchedule.of(1000d))
            .fxReset(FxResetCalculation.builder()
                .referenceCurrency(EUR)
                .index(ECB_EUR_GBP)
                .fixingOffset(MINUS_TWO_DAYS)
                .build())
            .build())
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_365F)
            .rate(ValueSchedule.of(0.025d))
            .build())
        .build();
    // expected
    RatePaymentPeriod rpp1 = RatePaymentPeriod.builder()
        .paymentDate(DATE_02_07)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_01_06)
            .endDate(DATE_02_05)
            .unadjustedStartDate(DATE_01_05)
            .yearFraction(ACT_365F.yearFraction(DATE_01_06, DATE_02_05))
            .rate(FixedRate.of(0.025d))
            .build())
        .currency(GBP)
        .notional(-1000d)
        .fxReset(FxReset.of(ECB_EUR_GBP, EUR, DATE_01_02))
        .build();
    RatePaymentPeriod rpp2 = RatePaymentPeriod.builder()
        .paymentDate(DATE_03_07)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_02_05)
            .endDate(DATE_03_05)
            .yearFraction(ACT_365F.yearFraction(DATE_02_05, DATE_03_05))
            .rate(FixedRate.of(0.025d))
            .build())
        .currency(GBP)
        .notional(-1000d)
        .fxReset(FxReset.of(ECB_EUR_GBP, EUR, DATE_02_03))
        .build();
    RatePaymentPeriod rpp3 = RatePaymentPeriod.builder()
        .paymentDate(DATE_04_09)
        .accrualPeriods(RateAccrualPeriod.builder()
            .startDate(DATE_03_05)
            .endDate(DATE_04_07)
            .unadjustedEndDate(DATE_04_05)
            .yearFraction(ACT_365F.yearFraction(DATE_03_05, DATE_04_07))
            .rate(FixedRate.of(0.025d))
            .build())
        .currency(GBP)
        .notional(-1000d)
        .fxReset(FxReset.of(ECB_EUR_GBP, EUR, DATE_03_03))
        .build();
    // assertion
    assertEquals(test.expand(), ExpandedSwapLeg.builder()
        .paymentPeriods(rpp1, rpp2, rpp3)
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_04_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentOffset(PLUS_TWO_DAYS)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, 1000d))
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_365F)
            .rate(ValueSchedule.of(0.025d))
            .build())
        .build();
    coverImmutableBean(test);
    RateCalculationSwapLeg test2 = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_02_05)
            .endDate(DATE_03_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentOffset(PLUS_THREE_DAYS)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, 2000d))
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_360)
            .rate(ValueSchedule.of(0.025d))
            .build())
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    RateCalculationSwapLeg test = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(DATE_01_05)
            .endDate(DATE_04_05)
            .frequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P1M)
            .paymentOffset(PLUS_TWO_DAYS)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, 1000d))
        .calculation(FixedRateCalculation.builder()
            .dayCount(DayCounts.ACT_365F)
            .rate(ValueSchedule.of(0.025d))
            .build())
        .build();
    assertSerialization(test);
  }

}