package com.neurowyzr.nw.dragon.service

final class SmokeTests
    extends SmokeTest with CreateMagicLinkHappyFlowTest with UpdateMagicLinkHappyFlowTest
    with InvalidateMagicLinkHappyFlowTest with CreateMagicLinkUnhappyFlowTest with UpdateMagicLinkUnhappyFlowTest
    with InvalidateMagicLinkUnhappyFlowTest with CreateUserHappyFlowTest with ConsumerFlowTest
