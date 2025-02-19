package com.just.donate.models

import com.just.donate.api.PaypalRoute.paypalAccountName
import com.just.donate.helper.OrganisationHelper.createNewRoots
import com.just.donate.mocks.config.AppConfigMock
import com.just.donate.mocks.config.AppConfigMock
import com.just.donate.models.errors.TransferError
import com.just.donate.utils.Money
import munit.FunSuite

class OrganisationTransferSuite extends FunSuite:

  val donor1Email = "donor1@example.org"
  val amountHundred = "100.00"
  val amountOneFifty = "150.00"

  test("transfer money between accounts") {
    var newRoots = createNewRoots()

    val donor = Donor(newRoots.getNewDonorId, "Donor1", donor1Email)
    val (donation, donationPart) = Donation(donor.id, Money(amountHundred))
    newRoots = newRoots.donate(donor, donationPart, donation, paypalAccountName, AppConfigMock()).toOption.get
    newRoots = newRoots.transfer(Money("50.00"), paypalAccountName, "Bank", AppConfigMock()).toOption.get

    assertEquals(newRoots.getAccount(paypalAccountName).get.totalBalance, Money("50.00"))
    assertEquals(newRoots.getAccount("Bank").get.totalBalance, Money("50.00"))
  }

  test("not transfer money if the source account does not have enough balance") {
    var newRoots = createNewRoots()

    val donor = Donor(newRoots.getNewDonorId, "Donor1", donor1Email)
    val (donation, donationPart) = Donation(donor.id, Money(amountHundred))
    newRoots = newRoots.donate(donor, donationPart, donation, paypalAccountName, AppConfigMock()).toOption.get

    assertEquals(
      newRoots.transfer(Money(amountOneFifty), paypalAccountName, "Bank", AppConfigMock()),
      Left(TransferError.INSUFFICIENT_ACCOUNT_FUNDS)
    )
  }

  test("not transfer money if the source account does not exist") {
    var newRoots = createNewRoots()

    val donor = Donor(newRoots.getNewDonorId, "Donor1", donor1Email)
    val (donation, donationPart) = Donation(donor.id, Money(amountHundred))
    newRoots = newRoots.donate(donor, donationPart, donation, paypalAccountName, AppConfigMock()).toOption.get

    assertEquals(
      newRoots.transfer(Money("50.00"), "NoExists", "Bank", AppConfigMock()),
      Left(TransferError.INVALID_ACCOUNT)
    )
  }

  test("not transfer money if the destination account does not exist") {
    var newRoots = createNewRoots()

    val donor = Donor(newRoots.getNewDonorId, "Donor1", donor1Email)
    val (donation, donationPart) = Donation(donor.id, Money(amountHundred))
    newRoots = newRoots.donate(donor, donationPart, donation, paypalAccountName, AppConfigMock()).toOption.get

    assertEquals(
      newRoots.transfer(Money("50.00"), paypalAccountName, "NoExists", AppConfigMock()),
      Left(TransferError.INVALID_ACCOUNT)
    )
  }

  test("not transfer money if the amount is negative") {
    var newRoots = createNewRoots()

    val donor = Donor(newRoots.getNewDonorId, "Donor1", donor1Email)
    val (donation, donationPart) = Donation(donor.id, Money(amountHundred))
    newRoots = newRoots.donate(donor, donationPart, donation, paypalAccountName, AppConfigMock()).toOption.get

    assertEquals(
      newRoots.transfer(Money("-50.00"), paypalAccountName, "Bank", AppConfigMock()),
      Left(TransferError.NON_POSITIVE_AMOUNT)
    )
  }

  test("not transfer money if the amount is zero") {
    var newRoots = createNewRoots()

    val donor = Donor(newRoots.getNewDonorId, "Donor1", donor1Email)
    val (donation, donationPart) = Donation(donor.id, Money(amountHundred))
    newRoots = newRoots.donate(donor, donationPart, donation, paypalAccountName, AppConfigMock()).toOption.get

    assertEquals(
      newRoots.transfer(Money("0.00"), paypalAccountName, "Bank", AppConfigMock()),
      Left(TransferError.NON_POSITIVE_AMOUNT)
    )
  }

  test("not transfer money if the source account is the same as the destination account") {
    var newRoots = createNewRoots()

    val donor = Donor(newRoots.getNewDonorId, "Donor1", donor1Email)
    val (donation, donationPart) = Donation(donor.id, Money(amountHundred))
    newRoots = newRoots.donate(donor, donationPart, donation, paypalAccountName, AppConfigMock()).toOption.get

    assertEquals(
      newRoots.transfer(Money("50.00"), paypalAccountName, paypalAccountName, AppConfigMock()),
      Left(TransferError.SAME_SOURCE_AND_DESTINATION_ACCOUNT)
    )
  }

  test("transfer earmarked money retains its earmarking after transfer") {
    var newRoots = createNewRoots()

    val educationEarmarking = Earmarking("Education", "Supporting education in Kenya")
    newRoots = newRoots.addEarmarking(educationEarmarking)

    val donor = Donor(newRoots.getNewDonorId, "Donor1", donor1Email)
    val (donation, donationPart) = Donation(donor.id, Money("200.00"), educationEarmarking)
    newRoots = newRoots.donate(donor, donationPart, donation, paypalAccountName, AppConfigMock()).toOption.get
    newRoots = newRoots.transfer(Money(amountHundred), paypalAccountName, "Bank", AppConfigMock()).toOption.get

    assertEquals(newRoots.getAccount(paypalAccountName).get.totalBalance, Money(amountHundred))
    assertEquals(newRoots.getAccount("Bank").get.totalBalance, Money(amountHundred))

    assertEquals(
      newRoots.getAccount(paypalAccountName).get.totalEarmarkedBalance(educationEarmarking),
      Money(amountHundred)
    )
    assertEquals(
      newRoots.getAccount("Bank").get.totalEarmarkedBalance(educationEarmarking),
      Money(amountHundred)
    )
  }

  test("transfers always the oldest donation if multiple are available") {
    var newRoots = createNewRoots()

    val educationEarmarking = Earmarking("Education", "Supporting education in Kenya")
    val healthEarmarking = Earmarking("Health", "Supporting health in Kenya")

    newRoots = newRoots.addEarmarking(educationEarmarking)
    newRoots = newRoots.addEarmarking(healthEarmarking)

    val donor = Donor(newRoots.getNewDonorId, "Donor1", donor1Email)
    val (donation, donationPart) = Donation(donor.id, Money(amountHundred), educationEarmarking)
    newRoots = newRoots.donate(donor, donationPart, donation, paypalAccountName, AppConfigMock()).toOption.get
    val donor2 = Donor(newRoots.getNewDonorId, "Donor2", "donor2@example.org")
    val (donation2, donationPart2) = Donation(donor2.id, Money(amountOneFifty), healthEarmarking)
    newRoots = newRoots.donate(donor2, donationPart2, donation2, paypalAccountName, AppConfigMock()).toOption.get

    newRoots = newRoots.transfer(Money("50.00"), paypalAccountName, "Bank", AppConfigMock()).toOption.get

    assertEquals(newRoots.getAccount(paypalAccountName).get.totalBalance, Money("200.00"))
    assertEquals(newRoots.getAccount("Bank").get.totalBalance, Money("50.00"))

    // Earmarked balances after partial transfer
    assertEquals(
      newRoots.getAccount(paypalAccountName).get.totalEarmarkedBalance(educationEarmarking),
      Money("50.00")
    )
    assertEquals(
      newRoots.getAccount(paypalAccountName).get.totalEarmarkedBalance(healthEarmarking),
      Money(amountOneFifty)
    )

    // The oldest donation (Education) is the one partially transferred
    assertEquals(
      newRoots.getAccount("Bank").get.totalEarmarkedBalance(educationEarmarking),
      Money("50.00")
    )
    assertEquals(
      newRoots.getAccount("Bank").get.totalEarmarkedBalance(healthEarmarking),
      Money("0.00")
    )
  }

  test("transfer multiple donation parts") {
    var newRoots = createNewRoots()

    val educationEarmarking = Earmarking("Education", "Supporting education in Kenya")
    newRoots = newRoots.addEarmarking(educationEarmarking)

    val donor = Donor(newRoots.getNewDonorId, "Donor1", donor1Email)
    val (donation, donationPart) = Donation(donor.id, Money(amountHundred), educationEarmarking)
    val (donation2, donationPart2) = Donation(donor.id, Money(amountOneFifty), educationEarmarking)
    newRoots = newRoots.donate(donor, donationPart, donation, paypalAccountName, AppConfigMock()).toOption.get
    newRoots = newRoots.donate(donor, donationPart2, donation2, paypalAccountName, AppConfigMock()).toOption.get

    newRoots = newRoots.transfer(Money("120.00"), paypalAccountName, "Bank", AppConfigMock()).toOption.get

    assertEquals(newRoots.getAccount(paypalAccountName).get.totalBalance, Money("130.00"))
    assertEquals(newRoots.getAccount("Bank").get.totalBalance, Money("120.00"))

    assertEquals(
      newRoots.getAccount(paypalAccountName).get.totalEarmarkedBalance(educationEarmarking),
      Money("130.00")
    )
    assertEquals(
      newRoots.getAccount("Bank").get.totalEarmarkedBalance(educationEarmarking),
      Money("120.00")
    )
  }
