package com.neurowyzr.nw.dragon.service.biz.models

import java.time.LocalDateTime

final case class User(id: Long,
                      username: String,
                      password: String,
                      firstName: String,
                      maybeEmailHash: Option[String],
                      maybeLastName: Option[String],
                      maybeDateOfBirth: Option[String],
                      maybeMobileNumber: Option[Long],
                      maybeCountry: Option[String],
                      maybeNationality: Option[String],
                      maybePostalCode: Option[Long],
                      maybeAddress: Option[String],
                      maybeGender: Option[String],
                      maybeUserStatus: Option[String],
                      maybeUserProfile: Option[String],
                      maybeIcOrPassportNumber: Option[String],
                      maybeExternalPatientRef: Option[String],
                      maybeSource: Option[String],
                      maybeUtcCreatedAt: Option[LocalDateTime],
                      maybeUtcUpdatedAt: Option[LocalDateTime]
                     )
    extends StringRepresentation

object User {

  def apply(username: String, password: String, firstName: String, source: String, externalPatientRef: String): User =
    User(
      id = Defaults.DefaultLongId,
      username = username: String,
      password = password: String,
      firstName = firstName: String,
      maybeEmailHash = None,
      maybeLastName = None,
      maybeDateOfBirth = None,
      maybeMobileNumber = None,
      maybeCountry = None,
      maybeNationality = None,
      maybePostalCode = None,
      maybeAddress = None,
      maybeGender = None,
      maybeUserStatus = Some(Defaults.DefaultUserStatus),
      maybeUserProfile = None,
      maybeIcOrPassportNumber = None,
      maybeExternalPatientRef = Some(externalPatientRef),
      maybeSource = Some(source),
      maybeUtcCreatedAt = None,
      maybeUtcUpdatedAt = None
    )

  def apply(username: String,
            password: String,
            firstName: String,
            source: String,
            externalPatientRef: String,
            country: String
           ): User = User(
    id = Defaults.DefaultLongId,
    username = username: String,
    password = password: String,
    firstName = firstName: String,
    maybeEmailHash = None,
    maybeLastName = None,
    maybeDateOfBirth = None,
    maybeMobileNumber = None,
    maybeCountry = Some(country),
    maybeNationality = None,
    maybePostalCode = None,
    maybeAddress = None,
    maybeGender = None,
    maybeUserStatus = Some(Defaults.DefaultUserStatus),
    maybeUserProfile = None,
    maybeIcOrPassportNumber = None,
    maybeExternalPatientRef = Some(externalPatientRef),
    maybeSource = Some(source),
    maybeUtcCreatedAt = None,
    maybeUtcUpdatedAt = None
  )

  def apply(username: String,
            password: String,
            firstName: String,
            lastName: String,
            maybeEmailHashed: Option[String],
            maybeMobile: Option[Long],
            dob: String,
            gender: String,
            source: String,
            externalPatientRef: String
           ): User = User(
    id = Defaults.DefaultLongId,
    username = username: String,
    password = password: String,
    firstName = firstName: String,
    maybeEmailHash = maybeEmailHashed,
    maybeLastName = Some(lastName),
    maybeDateOfBirth = Some(dob),
    maybeMobileNumber = maybeMobile,
    maybeCountry = None,
    maybeNationality = None,
    maybePostalCode = None,
    maybeAddress = None,
    maybeGender = Some(gender),
    maybeUserStatus = Some(Defaults.DefaultUserStatus),
    maybeUserProfile = None,
    maybeIcOrPassportNumber = None,
    maybeExternalPatientRef = Some(externalPatientRef),
    maybeSource = Some(source),
    maybeUtcCreatedAt = None,
    maybeUtcUpdatedAt = None
  )

}
