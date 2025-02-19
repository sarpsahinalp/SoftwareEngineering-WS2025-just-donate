package com.just.donate.mocks.paypal

import cats.effect.IO
import com.just.donate.db.mongo.MongoPaypalRepository
import com.just.donate.models.paypal.PayPalIPN
import org.mongodb.scala.{Document, MongoCollection, MongoDatabase}

class MongoPaypalRepositoryMock(collection: MongoCollection[Document]) extends MongoPaypalRepository(collection: MongoCollection[Document]) {
  var stored: List[PayPalIPN] = Nil

  override def findAll(): IO[Seq[PayPalIPN]] = IO.pure(stored)

  override def save(ipn: PayPalIPN): IO[PayPalIPN] = IO { 
    stored = stored :+ ipn
    ipn
  }

  override def findById(id: String): IO[Option[PayPalIPN]] = IO {
    stored.find(_.ipnTrackId == id)
  }

  def reset(): Unit = stored = Nil
}
