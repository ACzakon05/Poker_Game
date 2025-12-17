1. JAK URUCHOMIĆ:
-mvn clean install
-lobby: java -jar poker-server/target/poker-server-1.0-SNAPSHOT.jar 4
-w osobnych terminalach gracze: java -jar poker-client/target/poker-client-1.0-SNAPSHOT.jar
-dołącznie do gry dla każdego gracza: JOIN GAME=GAME_0 NAME=ADAM
-rozpoczęcie: pierwszy gracz (host) wpisuje komendę START
-przejście ANTE,DEAL: serwer sam pobiera ante i rozdaje karty
-BET1: wszyscy muszą wyrównac za pomocą komend CALL lub BET AMOUNT albo spasować czyli FOLD
-DRAW: gracze wymieniają karty za pomocą komendy DRAW CARDS=<indeksy kart> ( jeśli nie chcesz wymienić karty to 'DRAW CARDS=,')
-BET2: Można sprawdzić: CHECK, Można podbic BET AMOUNT, Można wyrównać CALL
-przejscie SHOWDOWN,PAYOUT. Zwycięzca otrzymuje żetony
-przejście END, LOBBY



mvn clean verify sonar:sonar   -Dsonar.projectKey=Poker-pz1   -Dsonar.projectName='Poker-pz1'   -Dsonar.host.url=http://localhost:9000   -Dsonar.token=sqp_3a9afaacb8ef8b21cebc7fe2692ac7f7a834570b
