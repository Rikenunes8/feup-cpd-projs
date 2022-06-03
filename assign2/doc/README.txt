# NOTA: em sistemas windows as barras "/" poderão ter que ser invertidas para "\"

# Para compilar o código basta, a partir da raiz do projeto 2, "assign2/", executar o comando:

javac src/membership/*.java src/messages/*.java src/utils/*.java src/store/*.java src/client/*.java -d build/


# Para remover o diretório com os pares chave-valor:

rm -rf network/     # em linux
rmdir /s /q network # em windows


# Para correr uma store é necessário inicializar primeiro o RMI registry num terminal à parte, dentro do diretório "assign2/build/" com o comando:

rmiregistry



# e, de seguida, em diferentes terminais, invocar as stores a partir do diretório "assign2/" por exemplo com os comandos:

java -cp build/ -Djava.rmi.server.codebase=file:./build store.Store 224.0.0.0 3000 127.0.0.1 9000
java -cp build/ -Djava.rmi.server.codebase=file:./build store.Store 224.0.0.0 3000 127.0.0.2 9000
java -cp build/ -Djava.rmi.server.codebase=file:./build store.Store 224.0.0.0 3000 127.0.0.3 9000
java -cp build/ -Djava.rmi.server.codebase=file:./build store.Store 224.0.0.0 3000 127.0.0.4 9000



# num terminal diferente dos anteriores pode-se invocar o cliente de teste com as variantes do comando:

java -cp build/ client.TestClient <ip>:<port> <opnd> [<filenameORkey>]

# exemplos:

java -cp build/ client.TestClient 127.0.0.1:9000 join
java -cp build/ client.TestClient 127.0.0.1:9000 put files/file1.txt
java -cp build/ client.TestClient 127.0.0.1:9000 get <key>
java -cp build/ client.TestClient 127.0.0.1:9000 delete <key>
java -cp build/ client.TestClient 127.0.0.1:9000 leave



# Os pares chave-valor correspondentes a cada nó são armazenados no diretório "assign2/network/"
