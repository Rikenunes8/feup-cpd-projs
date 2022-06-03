call ./test_client.bat 1 join
call ./test_client.bat 1 leave
call ./test_client.bat 2 join
call ./test_client.bat 1 join
call ./test_client.bat 3 join
call ./test_client.bat 4 join
call ./test_client.bat 3 leave
call ./test_client.bat 4 leave
call ./test_client.bat 2 leave
call ./test_client.bat 1 leave

call ./test_client.bat 1 join
call ./test_client.bat 1 put ../files/file1.txt
call ./test_client.bat 1 get 03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4
call ./test_client.bat 1 delete 03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4
call ./test_client.bat 1 get 03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4
call ./test_client.bat 1 leave

call ./test_client.bat 1 join
call ./test_client.bat 1 put ../files/file1.txt
call ./test_client.bat 2 join
call ./test_client.bat 2 get 03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4
call ./test_client.bat 2 delete 03
call ./test_client.bat 2 delete 03
call ./test_client.bat 2 get 03
call ./test_client.bat 2 put ../files/file2.txt
call ./test_client.bat 3 join
call ./test_client.bat 4 join
call ./test_client.bat 1 get 6c753a9f10127396157bdef3e1aa77ef2fc86689d6e921e75ed8bac66d98cc8f
call ./test_client.bat 4 get 6c753a9f10127396157bdef3e1aa77ef2fc86689d6e921e75ed8bac66d98cc8f
call ./test_client.bat 2 leave
call ./test_client.bat 2 get 6c753a9f10127396157bdef3e1aa77ef2fc86689d6e921e75ed8bac66d98cc8f
call ./test_client.bat 2 join
call ./test_client.bat 2 put ../files/file1.txt
call ./test_client.bat 2 put ../files/file3.txt
call ./test_client.bat 2 put ../files/file4.txt
call ./test_client.bat 2 put ../files/file5.txt
call ./test_client.bat 4 leave
call ./test_client.bat 4 join
