package com.zunf.tankbattletcpserver.manager;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class GameMatchManager {

    AtomicLong atomicInteger = new AtomicLong(0);

}
