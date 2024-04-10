#!/bin/bash

IS_GREEN=$(docker ps | grep green) # 현재 실행중인 App이 blue인지 확인합니다.

if [ -z "$IS_GREEN" ]; then # blue라면

  echo "### BLUE => GREEN ###"

  echo "1. get green image"
  docker compose pull gateway-green # green으로 이미지를 내려받습니다.

  echo "2. green container up"
  docker compose up -d gateway-green # green 컨테이너 실행

  for cnt in {1..10}
  do
    echo "3. green health check..."
    echo "서버 응답 확인중(${cnt}/10)";

    STATUS=$(curl http://127.0.0.1:8081/healthCheck)
      if [ "$STATUS" -eq 200 ]
        then
          echo "health check success"
          break ;
        else
          sleep 10

      fi
  done;

  if [ $cnt -eq 10 ]
  then
  	echo "서버가 정상적으로 구동되지 않았습니다."
  	exit 1
  fi

  echo "4. reload nginx"
  sudo cp /etc/nginx/conf.d/gateway-green-url /etc/nginx/conf.d/service-url.inc
  sudo nginx -s reload

  echo "5. blue container down"
  docker compose stop gateway-blue
else
  echo "### GREEN => BLUE ###"

  echo "1. get blue image"
  docker compose pull gateway-blue

  echo "2. blue container up"
  docker compose up -d gateway-blue

  for cnt in {1..10}
  do
    echo "3. blue health check..."
    echo "서버 응답 확인중(${cnt}/10)";

    STATUS=$(curl http://127.0.0.1:8080/healthCheck)
    if [ "$STATUS" -eq 200 ]
      then
        echo "health check success"
        break ;
      else
        sleep 10

    fi
  done;

  if [ $cnt -eq 10 ]
  then
  	echo "서버가 정상적으로 구동되지 않았습니다."
  	exit 1
  fi

  echo "4. reload nginx"
  sudo cp /etc/nginx/conf.d/gateway-blue-url /etc/nginx/conf.d/service-url.inc
  sudo nginx -s reload

  echo "5. green container down"
  docker compose stop gateway-green
fi