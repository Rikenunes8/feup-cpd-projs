#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <fstream>
#include <time.h>
#include <cstdlib>
#include <papi.h>

using namespace std;

#define SYSTEMTIME clock_t

 
double OnMult(int n) {
	
	SYSTEMTIME Time1, Time2;
	
	char st[100];
	double temp;
	int i, j, k;

	double *pha, *phb, *phc;
	

  pha = (double *)malloc((n * n) * sizeof(double));
	phb = (double *)malloc((n * n) * sizeof(double));
	phc = (double *)malloc((n * n) * sizeof(double));

	for(i=0; i<n; i++) {
		for(j=0; j<n; j++) {
			pha[i*n + j] = (double)1.0;
    }
  }


	for(i=0; i<n; i++) {
    for(j=0; j<n; j++) {
			phb[i*n + j] = (double)(i+1);
    }
  }



  Time1 = clock();

	for(i=0; i<n; i++) {	
    for( j=0; j<n; j++) {	
      temp = 0;
			for( k=0; k<n; k++) {	
				temp += pha[i*n+k] * phb[k*n+j];
			}
			phc[i*n+j]=temp;
		}
	}


  Time2 = clock();
	double time = (double)(Time2 - Time1) / CLOCKS_PER_SEC;
	sprintf(st, "Time: %3.3f seconds\n", time);
	cout << st;

	// display 10 elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for(i=0; i<1; i++) {	
    for(j=0; j<min(10,n); j++) {
      cout << phc[j] << " ";
    }
	}
	cout << endl;

  free(pha);
  free(phb);
  free(phc);
	
	return time;
}

// add code here for line x line matriz multiplication
double OnMultLine(int n) {
  
  SYSTEMTIME Time1, Time2;
	
	char st[100];
	double temp;
	int i, j, k;

	double *pha, *phb, *phc;
	

  pha = (double *)malloc((n * n) * sizeof(double));
	phb = (double *)malloc((n * n) * sizeof(double));
	phc = (double *)malloc((n * n) * sizeof(double));

	for(i=0; i<n; i++) {
		for(j=0; j<n; j++) {
			pha[i*n + j] = (double)1.0;
    }
  }


	for(i=0; i<n; i++) {
		for(j=0; j<n; j++) {
			phb[i*n + j] = (double)(i+1);
    }
  }

  for(i=0; i<n; i++) {
		for(j=0; j<n; j++) {
			phc[i*n + j] = (double)0;
    }
  }



  Time1 = clock();

	for(i=0; i<n; i++) {	
    for( k=0; k<n; k++) {	
      for( j=0; j<n; j++) {	
				phc[i*n+j] += pha[i*n+k] * phb[k*n+j];
			}
		}
	}


  Time2 = clock();
	double time = (double)(Time2 - Time1) / CLOCKS_PER_SEC;
	sprintf(st, "Time: %3.3f seconds\n", time);
	cout << st;



	// display 10 elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for(i=0; i<1; i++) {	
    for(j=0; j<min(10,n); j++) {
      cout << phc[j] << " ";
    }
	}
	cout << endl;

	free(pha);
	free(phb);
	free(phc);
	return time;
}

// add code here for block x block matriz multiplication
double OnMultBlock(int n, int bkSize) {
    
  SYSTEMTIME Time1, Time2;
	
	char st[100];
	double temp;
	int i, j, k, l, c, t;

	double *pha, *phb, *phc;
	

  pha = (double *)malloc((n * n) * sizeof(double));
	phb = (double *)malloc((n * n) * sizeof(double));
	phc = (double *)malloc((n * n) * sizeof(double));

	for(i=0; i<n; i++) {
		for(j=0; j<n; j++) {
			pha[i*n + j] = (double)1.0;
    }
  }


	for(i=0; i<n; i++) {
		for(j=0; j<n; j++) {
			phb[i*n + j] = (double)(i+1);
    }
  }

  for(i=0; i<n; i++) {
		for(j=0; j<n; j++) {
			phc[i*n + j] = 0.0;
    }
  }
	int nb = n/bkSize;
  cout << "nb: " << nb << endl;
  cout << "bkSize: " << bkSize << endl;
  cout << "n: " << n << endl;

  Time1 = clock();

	for(i = 0; i < nb; i++){
		for(j = 0; j < nb; j++){
			for(k = 0; k < nb; k++){

				for(l = 0; l < bkSize; l++){
					for(t = 0; t < bkSize; t++){
					  for(c = 0; c < bkSize; c++){
							phc[(i*bkSize + l) * n + (j*bkSize + c)] += pha[(i*bkSize + l) * n + (k*bkSize + t)] * phb[(k*bkSize + t) * n + (j*bkSize + c)];
						}
					}
				}
			}
		}
	}

  Time2 = clock();
  
  double time = (double)(Time2 - Time1) / CLOCKS_PER_SEC;
	sprintf(st, "Time: %3.3f seconds\n", time);
	cout << st;

	// display 10 elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for(i=0; i<1; i++) {	
    for(j=0; j<min(10,n); j++) {
      cout << phc[j] << " ";
    }
	}
	cout << endl;

  free(pha);
  free(phb);
  free(phc);

  return time;
}



void handle_error (int retval)
{
  printf("PAPI error %d: %s\n", retval, PAPI_strerror(retval));
  exit(1);
}

void init_papi() {
  int retval = PAPI_library_init(PAPI_VER_CURRENT);
  if (retval != PAPI_VER_CURRENT && retval < 0) {
    printf("PAPI library version mismatch!\n");
    exit(1);
  }
  if (retval < 0) handle_error(retval);

  std::cout << "PAPI Version Number: MAJOR: " << PAPI_VERSION_MAJOR(retval)
            << " MINOR: " << PAPI_VERSION_MINOR(retval)
            << " REVISION: " << PAPI_VERSION_REVISION(retval) << "\n";
}

void add_event(int EventSet, int event, string error) {
  int ret = PAPI_add_event(EventSet,event );
	if (ret != PAPI_OK) cout << "ERROR: " << error << endl;
}

void remove_event(int EventSet, int event) {
  int ret = PAPI_remove_event( EventSet, event);
	if ( ret != PAPI_OK )
		std::cout << "FAIL remove event" << endl; 
}


int main (int argc, char *argv[])
{

	char c;
	int n, blockSize = 0;
	int op;
	
	int EventSet = PAPI_NULL;
  long long values[5];
  int ret;
	double time;

	std::ofstream outfile;

  outfile.open("times.csv", std::ios_base::app); // append instead of overwrite 
	outfile << "Method,MatrixSize,BlockSize,Duration,PAPI_L1_DCM,PAPI_L2_DCM,PAPI_TOT_CYC,PAPI_TOT_INS,PAPI_FP_INS,CPI,Gflops,L1&L2_TOT_MISS" << endl;

	ret = PAPI_library_init( PAPI_VER_CURRENT );
	if ( ret != PAPI_VER_CURRENT )
		std::cout << "FAIL" << endl;


	ret = PAPI_create_eventset(&EventSet);
  if (ret != PAPI_OK) cout << "ERROR: create eventset" << endl;


	add_event(EventSet, PAPI_L1_DCM , "PAPI_L1_DCM" );
  add_event(EventSet, PAPI_L2_DCM , "PAPI_L2_DCM" );
  add_event(EventSet, PAPI_TOT_CYC, "PAPI_TOT_CYC");
  add_event(EventSet, PAPI_TOT_INS, "PAPI_TOT_INS");
  add_event(EventSet, PAPI_FP_INS , "PAPI_FP_OPS" );

	op=1;
	do {
		cout << endl << "0. Exit" << endl;
		cout << "1. Multiplication" << endl;
		cout << "2. Line Multiplication" << endl;
		cout << "3. Block Multiplication" << endl;
		cout << "Selection?: ";
		cin >>op;
		if (op == 0)
			break;
		printf("Dimensions: lins=cols ? ");
    cin >> n;


		// Start counting
		ret = PAPI_start(EventSet);
		if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;

		switch (op){
			case 1: 
				time = OnMult(n);
				break;
			case 2:
				time = OnMultLine(n);  
				break;
			case 3:
				cout << "Block Size? ";
				cin >> blockSize;
				time = OnMultBlock(n, blockSize);  
				break;
		}

    ret = PAPI_stop(EventSet, values);
    if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;

    // printf("L1 DCM: %lld \n",values[0]);
    // printf("L2 DCM: %lld \n",values[1]);
    // printf("TOT_CYC: %lld \n",values[2]);
    // printf("TOT_INS: %lld \n",values[3]);
    // printf("FP_OPS: %lld \n",values[4]);

		double CPI = values[2] / values[3];
    double Gflops = values[4] / time * 10e9;
    unsigned int l1l2_tot_miss = values[0] + values[1];
		outfile << op << "," << n << "," << blockSize << "," << time << "," << values[0] << "," << values[1] << "," << values[2] << "," << values[3] << "," << CPI << "," << Gflops << "," << l1l2_tot_miss<< endl;

		ret = PAPI_reset( EventSet );
		if ( ret != PAPI_OK )
			std::cout << "FAIL reset" << endl; 

	}while (op != 0);

	remove_event( EventSet, PAPI_L1_DCM );
  remove_event( EventSet, PAPI_L2_DCM );
	remove_event( EventSet, PAPI_TOT_CYC);
	remove_event( EventSet, PAPI_TOT_INS);
	remove_event( EventSet, PAPI_FP_OPS );

	ret = PAPI_destroy_eventset( &EventSet );
	if ( ret != PAPI_OK )
		std::cout << "FAIL destroy" << endl;

  outfile.close();
}