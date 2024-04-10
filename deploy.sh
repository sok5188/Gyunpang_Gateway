#!/bin/bash

IS_GREEN=$(docker ps | grep green) # 현재 실행중인 App이 blue인지 확인합니다.

if [ -z "$IS_GREEN" ]; then # blue라면
  echo "### BLUE => GREEN ###"

  OLD_CONTAINER='gateway-blue'
  NEW_CONTAINER='gateway-green'
  NEW_PORT='8081'

else
  echo "### GREEN => BLUE ###"

  OLD_CONTAINER='be-green'
  NEW_CONTAINER='be-blue'
  NEW_PORT='8080'
fi

echo "1. get new image"
docker compose pull $NEW_CONTAINER

echo "2. new container up"
docker compose up -d $NEW_CONTAINER

for cnt in {1..10}
do
  echo "3. new container health check..."
  echo "서버 응답 확인중(${cnt}/10)";

  STATUS=$(curl http://127.0.0.1:${NEW_PORT}/healthCheck)
    if [ $STATUS -eq 200 ]
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
sudo cp /etc/nginx/conf.d/${NEW_CONTAINER}-url /etc/nginx/conf.d/service-url.inc
sudo nginx -s reload

echo "5. old container down"
docker compose stop $OLD_CONTAINER