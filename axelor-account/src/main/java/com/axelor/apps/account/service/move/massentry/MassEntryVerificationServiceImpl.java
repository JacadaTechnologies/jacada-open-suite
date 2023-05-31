package com.axelor.apps.account.service.move.massentry;

import com.axelor.apps.account.db.Journal;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.MoveLine;
import com.axelor.apps.account.db.MoveLineMassEntry;
import com.axelor.apps.account.db.PaymentCondition;
import com.axelor.apps.account.db.PaymentMode;
import com.axelor.apps.account.db.repo.JournalTypeRepository;
import com.axelor.apps.account.db.repo.MoveLineMassEntryRepository;
import com.axelor.apps.account.exception.AccountExceptionMessage;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.move.MoveControlService;
import com.axelor.apps.account.service.move.MoveLineControlService;
import com.axelor.apps.account.service.move.MoveValidateService;
import com.axelor.apps.account.service.moveline.MoveLineToolService;
import com.axelor.apps.account.service.moveline.massentry.MoveLineMassEntryToolService;
import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.BankDetails;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Period;
import com.axelor.apps.base.db.repo.PeriodRepository;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.apps.base.db.repo.YearRepository;
import com.axelor.apps.base.exceptions.BaseExceptionMessage;
import com.axelor.apps.base.service.PeriodService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.common.ObjectUtils;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MassEntryVerificationServiceImpl implements MassEntryVerificationService {

  private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected PeriodService periodService;
  protected MoveLineToolService moveLineToolService;
  protected MoveLineControlService moveLineControlService;
  protected MoveValidateService moveValidateService;
  protected MoveControlService moveControlService;
  protected MoveLineMassEntryToolService moveLineMassEntryToolService;
  protected AppAccountService appAccountService;

  @Inject
  public MassEntryVerificationServiceImpl(
      PeriodService periodService,
      MoveLineToolService moveLineToolService,
      MoveLineControlService moveLineControlService,
      MoveValidateService moveValidateService,
      MoveControlService moveControlService,
      MoveLineMassEntryToolService moveLineMassEntryToolService,
      AppAccountService appAccountService) {
    this.periodService = periodService;
    this.moveLineToolService = moveLineToolService;
    this.moveLineControlService = moveLineControlService;
    this.moveValidateService = moveValidateService;
    this.moveControlService = moveControlService;
    this.moveLineMassEntryToolService = moveLineMassEntryToolService;
    this.appAccountService = appAccountService;
  }

  @Override
  public void checkPreconditionsMassEntry(
      Move move, int temporaryMoveNumber, List<Move> massEntryMoveList, boolean manageCutOff) {
    this.checkDateMassEntryMove(move, temporaryMoveNumber);
    this.checkOriginDateMassEntryMove(move, temporaryMoveNumber);
    this.checkOriginMassEntryMoveLines(move, temporaryMoveNumber, massEntryMoveList);
    this.checkCurrencyRateMassEntryMove(move, temporaryMoveNumber);
    this.checkPartnerMassEntryMove(move, temporaryMoveNumber);
    this.checkWellBalancedMove(move, temporaryMoveNumber);
    this.checkCutOffMassEntryMove(move, temporaryMoveNumber, manageCutOff);
  }

  @Override
  public void checkChangesMassEntryMoveLine(
      MoveLineMassEntry moveLine,
      Move parentMove,
      MoveLineMassEntry newMoveLine,
      boolean manageCutOff)
      throws AxelorException {

    // Check move line mass entry date
    LocalDate newDate = newMoveLine.getDate();
    Company company = parentMove.getCompany();
    if (!moveLine.getDate().equals(newDate)) {
      moveLine.setDate(newDate);

      Period period;
      if (newDate != null && company != null) {
        period = periodService.getActivePeriod(newDate, company, YearRepository.TYPE_FISCAL);
        parentMove.setPeriod(period);
      }
      moveLineToolService.checkDateInPeriod(parentMove, moveLine);
    }

    // Check move line mass entry originDate
    LocalDate newOriginDate = newMoveLine.getOriginDate();
    if (moveLine.getOriginDate() == null || !moveLine.getOriginDate().equals(newOriginDate)) {
      moveLine.setOriginDate(newOriginDate);
      if (manageCutOff) {
        moveLine.setCutOffStartDate(newOriginDate);
        moveLine.setCutOffEndDate(newOriginDate);
      }
    }

    // Check move line mass entry origin
    String newOrigin = newMoveLine.getOrigin() != null ? newMoveLine.getOrigin() : "";
    if (!newOrigin.equals(moveLine.getOrigin())) {
      moveLine.setOrigin(newOrigin);
    }

    // Check move line mass entry move description
    String newMoveDescription =
        newMoveLine.getMoveDescription() != null ? newMoveLine.getMoveDescription() : "";
    if (!newMoveDescription.equals(moveLine.getMoveDescription())) {
      if (moveLine.getMoveDescription().equals(moveLine.getDescription())) {
        moveLine.setDescription(newMoveDescription);
      }
      moveLine.setMoveDescription(newMoveDescription);
    }

    // Check move line mass entry payment mode
    if (appAccountService.getAppAccount().getAllowMultiInvoiceTerms()) {
      PaymentMode newMovePaymentMode = newMoveLine.getMovePaymentMode();
      if (moveLine.getMovePaymentMode() == null
          || !moveLine.getMovePaymentMode().equals(newMovePaymentMode)) {
        moveLine.setMovePaymentMode(newMovePaymentMode);
      }
    }

    // Check move line mass entry currency rate
    BigDecimal newCurrencyRate = newMoveLine.getCurrencyRate();
    if (!newCurrencyRate.equals(moveLine.getCurrencyRate())) {
      moveLine.setCurrencyRate(newCurrencyRate);
    }

    // Check move line mass entry payment condition
    PaymentCondition newPaymentCondition = newMoveLine.getMovePaymentCondition();
    if (moveLine.getMovePaymentCondition() == null
        || !moveLine.getMovePaymentCondition().equals(newPaymentCondition)) {
      moveLine.setMovePaymentCondition(newPaymentCondition);
    }

    // Check move line mass entry VatSystemSelect
    int newVatSystemSelect = newMoveLine.getVatSystemSelect();
    if (!moveLine.getVatSystemSelect().equals(newVatSystemSelect)
        && moveLine.getIsEdited() == MoveLineMassEntryRepository.MASS_ENTRY_IS_EDITED_ALL) {
      moveLine.setVatSystemSelect(newVatSystemSelect);
    }

    // Check move line mass entry partner
    if (parentMove.getJournal().getJournalType().getTechnicalTypeSelect()
            == JournalTypeRepository.TECHNICAL_TYPE_SELECT_EXPENSE
        && !moveLine.getPartner().equals(newMoveLine.getPartner())) {
      moveLineMassEntryToolService.setPartnerChanges(moveLine, newMoveLine);
    }

    // Check move line mass entry partner bank details
    BankDetails newPartnerBankDetails = newMoveLine.getMovePartnerBankDetails();
    if (moveLine.getMovePartnerBankDetails() != null
        && !moveLine.getMovePartnerBankDetails().equals(newPartnerBankDetails)) {
      moveLine.setMovePartnerBankDetails(newPartnerBankDetails);
    }
  }

  @Override
  public void checkDateMassEntryMove(Move move, int temporaryMoveNumber) {
    boolean hasDateError;

    MoveLineMassEntry firstMoveLine = move.getMoveLineMassEntryList().get(0);

    for (MoveLineMassEntry moveLine : move.getMoveLineMassEntryList()) {
      hasDateError = false;
      if (move.getPeriod() == null) {
        hasDateError = true;
        this.setMassEntryErrorMessage(
            move,
            String.format(
                I18n.get(BaseExceptionMessage.PERIOD_1), move.getCompany(), move.getDate()),
            true,
            temporaryMoveNumber);
      } else {
        if (move.getPeriod().getStatusSelect() == PeriodRepository.STATUS_CLOSED
            || move.getPeriod().getStatusSelect() == PeriodRepository.STATUS_CLOSURE_IN_PROGRESS) {
          hasDateError = true;
          this.setMassEntryErrorMessage(
              move,
              I18n.get(AccountExceptionMessage.MOVE_PERIOD_IS_CLOSED),
              true,
              temporaryMoveNumber);
        }
      }

      if (!firstMoveLine.getDate().equals(moveLine.getDate())) {
        hasDateError = true;
        this.setMassEntryErrorMessage(
            move,
            I18n.get(AccountExceptionMessage.MASS_ENTRY_DIFFERENT_MOVE_LINE_DATE),
            true,
            temporaryMoveNumber);
      }

      if (hasDateError) {
        this.setFieldsErrorListMessage(moveLine, "date");
      }
    }
  }

  @Override
  public void checkCurrencyRateMassEntryMove(Move move, int temporaryMoveNumber) {
    boolean errorAdded = false;

    for (MoveLineMassEntry moveLine : move.getMoveLineMassEntryList()) {
      if (BigDecimal.ZERO
          .setScale(AppBaseService.DEFAULT_NB_DECIMAL_DIGITS)
          .equals(
              moveLine
                  .getCurrencyRate()
                  .setScale(AppBaseService.DEFAULT_NB_DECIMAL_DIGITS, RoundingMode.HALF_UP))) {
        this.setFieldsErrorListMessage(moveLine, "currencyRate");
        if (!errorAdded) {
          errorAdded = true;
        }
      }
    }
    this.setMassEntryErrorMessage(
        move,
        I18n.get(AccountExceptionMessage.MASS_ENTRY_CURRENCY_RATE_NULL),
        errorAdded,
        temporaryMoveNumber);
  }

  @Override
  public void checkOriginDateMassEntryMove(Move move, int temporaryMoveNumber) {
    boolean errorAdded = false;

    MoveLineMassEntry firstMoveLine = move.getMoveLineMassEntryList().get(0);
    for (MoveLineMassEntry moveLine : move.getMoveLineMassEntryList()) {
      if (firstMoveLine.getOriginDate() != null
          && !firstMoveLine.getOriginDate().equals(moveLine.getOriginDate())) {
        this.setFieldsErrorListMessage(moveLine, "originDate");
      }
    }
    this.setMassEntryErrorMessage(
        move,
        I18n.get(AccountExceptionMessage.MASS_ENTRY_DIFFERENT_MOVE_LINE_ORIGIN_DATE),
        errorAdded,
        temporaryMoveNumber);
  }

  @Override
  public void checkOriginMassEntryMoveLines(
      Move move, int temporaryMoveNumber, List<Move> massEntryMoveList) {
    try {
      moveControlService.checkDuplicateOrigin(move);
      this.checkDuplicateOriginMassEntryMoveList(massEntryMoveList, temporaryMoveNumber, move);
    } catch (AxelorException e) {
      this.setErrorMassEntryMoveLines(move, temporaryMoveNumber, "origin", e.getMessage());
    }
  }

  protected void checkDuplicateOriginMassEntryMoveList(
      List<Move> massEntryMoveList, int temporaryMoveNumber, Move move) throws AxelorException {
    if (move.getJournal() != null
        && move.getPartner() != null
        && move.getJournal().getHasDuplicateDetectionOnOrigin()) {
      String moveIdList =
          massEntryMoveList.stream()
              .filter(
                  ml ->
                      ObjectUtils.notEmpty(ml.getMoveLineMassEntryList())
                          && ml.getMoveLineMassEntryList().get(0).getTemporaryMoveNumber()
                              != temporaryMoveNumber
                          && ml.getOrigin().equals(move.getOrigin()))
              .map(Move::getReference)
              .collect(Collectors.joining(","));
      if (ObjectUtils.notEmpty(moveIdList)) {
        throw new AxelorException(
            move,
            TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
            I18n.get(AccountExceptionMessage.MOVE_DUPLICATE_ORIGIN_BLOCKING_MESSAGE),
            moveIdList,
            move.getPartner().getFullName(),
            move.getPeriod().getYear().getName());
      }
    }
  }

  @Override
  public void checkPartnerMassEntryMove(Move move, int temporaryMoveNumber) {
    boolean errorAdded = false;
    int[] technicalTypeSelectArray = {
      JournalTypeRepository.TECHNICAL_TYPE_SELECT_EXPENSE,
      JournalTypeRepository.TECHNICAL_TYPE_SELECT_SALE,
      JournalTypeRepository.TECHNICAL_TYPE_SELECT_CREDIT_NOTE
    };
    StringBuilder differentPartner = new StringBuilder();

    if (move.getJournal() != null
        && ArrayUtils.contains(
            technicalTypeSelectArray,
            move.getJournal().getJournalType().getTechnicalTypeSelect())) {
      for (MoveLineMassEntry moveLine : move.getMoveLineMassEntryList()) {
        try {
          moveLineControlService.checkPartner(moveLine);
        } catch (AxelorException e) {
          this.setFieldsErrorListMessage(moveLine, "partner");
          errorAdded = true;
          differentPartner.append(",").append(moveLine.getPartner().getName());
        }
      }
      this.setMassEntryErrorMessage(
          move,
          String.format(
              I18n.get(AccountExceptionMessage.MOVE_LINE_INCONSISTENCY_DETECTED_PARTNER),
              differentPartner,
              move.getPartner()),
          errorAdded,
          temporaryMoveNumber);
    }
  }

  public void checkCutOffMassEntryMove(Move move, int temporaryMoveNumber, boolean manageCutOff) {
    boolean hasEmptyCutOff =
        move.getMoveLineList().stream()
            .anyMatch(
                ml ->
                    ObjectUtils.notEmpty(ml.getAccount())
                        && ml.getAccount().getManageCutOffPeriod()
                        && (ObjectUtils.isEmpty(ml.getCutOffStartDate())
                            || ObjectUtils.isEmpty(ml.getCutOffEndDate())));

    boolean addCutOffError =
        !manageCutOff
            && hasEmptyCutOff
            && appAccountService.getAppAccount().getManageCutOffPeriod();

    this.setMassEntryErrorMessage(
        move,
        I18n.get(AccountExceptionMessage.MOVE_MISSING_CUT_OFF_DATE),
        addCutOffError,
        temporaryMoveNumber);
  }

  private void setMassEntryErrorMessage(
      Move move, String message, boolean toSet, int temporaryMoveNumber) {
    String massEntryErrors = move.getMassEntryErrors();
    StringBuilder finalMessage = new StringBuilder();

    if (toSet) {
      if (ObjectUtils.isEmpty(massEntryErrors)) {
        finalMessage
            .append(
                String.format(
                    I18n.get(AccountExceptionMessage.MASS_ENTRY_MOVE_IDENTIFICATION_MESSAGE),
                    temporaryMoveNumber))
            .append('\n');
        massEntryErrors = "";
      }
      finalMessage.append(message).append('\n');
      move.setMassEntryErrors(massEntryErrors + finalMessage);
    }
  }

  private void setFieldsErrorListMessage(MoveLineMassEntry moveLine, String fieldName) {
    StringBuilder message = new StringBuilder();

    if (ObjectUtils.notEmpty(moveLine.getFieldsErrorList())) {
      message.append(moveLine.getFieldsErrorList()).append(';');
    }
    message.append(fieldName).append(':');

    switch (fieldName) {
      case "date":
        message.append(moveLine.getDate().toString());
        break;
      case "currencyRate":
        message.append(moveLine.getCurrencyRate().toString());
        break;
      case "originDate":
        message.append(moveLine.getOriginDate().toString());
        break;
      case "origin":
        message.append(moveLine.getOrigin());
        break;
      case "partner":
        message.append(moveLine.getPartner().getName());
        break;
    }

    moveLine.setFieldsErrorList(message.toString());
  }

  @Override
  public void checkWellBalancedMove(Move move, int temporaryMoveNumber) {
    try {
      moveValidateService.validateWellBalancedMove(move);
      moveValidateService.checkTaxAmount(move);
    } catch (AxelorException e) {
      this.setErrorMassEntryMoveLines(move, temporaryMoveNumber, "balance", e.getMessage());
    }
  }

  public void checkAccountAnalytic(Move move, int temporaryMoveNumber) {
    String message = "";
    int lineCount = 0;

    for (MoveLine moveLine : move.getMoveLineList()) {
      try {
        moveLineControlService.checkAccountAnalytic(move, moveLine, moveLine.getAccount());
      } catch (AxelorException e) {
        lineCount++;
        for (MoveLineMassEntry element : move.getMoveLineMassEntryList()) {
          if (Objects.equals(element.getTemporaryMoveNumber(), temporaryMoveNumber)
              && Objects.equals(element.getCounter(), moveLine.getCounter())) {
            this.setFieldsErrorListMessage(element, "analytic:");
            break;
          }
        }
        if (Objects.equals(TraceBackRepository.CATEGORY_CONFIGURATION_ERROR, e.getCategory())) {
          message = String.format(I18n.get(AccountExceptionMessage.MOVE_11), lineCount);
        } else if (Objects.equals(TraceBackRepository.CATEGORY_MISSING_FIELD, e.getCategory())) {
          message =
              String.format(
                  I18n.get(AccountExceptionMessage.MOVE_10), moveLine.getAccount(), lineCount);
        }
        this.setMassEntryErrorMessage(move, message, true, temporaryMoveNumber);
      }
    }
  }

  @Override
  public void setErrorMassEntryMoveLines(
      Move move, int temporaryMoveNumber, String fieldName, String errorMessage) {
    boolean errorAdded = false;
    for (MoveLineMassEntry element : move.getMoveLineMassEntryList()) {
      if (Objects.equals(element.getTemporaryMoveNumber(), temporaryMoveNumber)) {
        this.setFieldsErrorListMessage(element, fieldName);
        errorAdded = true;
      }
    }
    this.setMassEntryErrorMessage(move, errorMessage, errorAdded, temporaryMoveNumber);
  }

  @Override
  public void verifyCompanyBankDetails(
      Move move, Company company, BankDetails companyBankDetails, Journal journal)
      throws AxelorException {
    BankDetails newCompanyBankDetails = companyBankDetails;
    int technicalTypeSelect = journal.getJournalType().getTechnicalTypeSelect();

    if (company.getDefaultBankDetails() == null
        && (technicalTypeSelect == JournalTypeRepository.TECHNICAL_TYPE_SELECT_EXPENSE
            || technicalTypeSelect == JournalTypeRepository.TECHNICAL_TYPE_SELECT_SALE)) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
          String.format(
              I18n.get(AccountExceptionMessage.COMPANY_BANK_DETAILS_MISSING), company.getName()));

    } else if (company.getDefaultBankDetails() != null) {
      if (newCompanyBankDetails == null
          && technicalTypeSelect != JournalTypeRepository.TECHNICAL_TYPE_SELECT_TREASURY) {
        newCompanyBankDetails = company.getDefaultBankDetails();
      } else if (newCompanyBankDetails != null
          && technicalTypeSelect == JournalTypeRepository.TECHNICAL_TYPE_SELECT_TREASURY
          && newCompanyBankDetails.getJournal() != journal) {
        newCompanyBankDetails = null;
      }
    }

    move.setCompanyBankDetails(newCompanyBankDetails);
  }
}