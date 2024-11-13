package com.neurowyzr.nw.dragon.service.biz

import com.neurowyzr.nw.dragon.service.biz.models.InvalidateMagicLinkTask
import com.neurowyzr.nw.finatra.lib.pipeline.FPipeline

trait InvalidateMagicLinkTaskPipeline extends FPipeline[InvalidateMagicLinkTask]
