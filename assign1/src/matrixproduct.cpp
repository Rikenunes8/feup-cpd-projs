#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <fstream>
#include <time.h>
#include <cstdlib>
#include <papi.h>

using namespace std;

#define SYSTEMTIME clock_t

 
double OnMult(int m_ar, int m_br) {
	
	SYSTEMTIME Time1, Time2;
	
	char st[100];
	double temp;
	int i, j, k;

	double *pha, *phb, *phc;
	

  	pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for(i=0; i<m_ar; i++) {
		for(j=0; j<m_ar; j++) {
			pha[i*m_ar + j] = (double)1.0;
    	}
  	}


	for(i=0; i<m_br; i++) {
		for(j=0; j<m_br; j++) {
			phb[i*m_br + j] = (double)(i+1);
    	}
  	}



  	Time1 = clock();

	for(i=0; i<m_ar; i++) {	
    for( j=0; j<m_br; j++) {	
      temp = 0;
			for( k=0; k<m_ar; k++) {	
				temp += pha[i*m_ar+k] * phb[k*m_br+j];
			}
			phc[i*m_ar+j]=temp;
		}
	}


  	Time2 = clock();
	double time = (double)(Time2 - Time1) / CLOCKS_PER_SEC;
	sprintf(st, "Time: %3.3f seconds\n", time);
	cout << st;

	// display 10 elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for(i=0; i<1; i++) {	
    for(j=0; j<min(10,m_br); j++) {
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
double OnMultLine(int m_ar, int m_br) {
  
  SYSTEMTIME Time1, Time2;
	
	char st[100];
	double temp;
	int i, j, k;

	double *pha, *phb, *phc;
	

  pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for(i=0; i<m_ar; i++) {
		for(j=0; j<m_ar; j++) {
			pha[i*m_ar + j] = (double)1.0;
    }
  }


	for(i=0; i<m_br; i++) {
		for(j=0; j<m_br; j++) {
			phb[i*m_br + j] = (double)(i+1);
    }
  }

  for(i=0; i<m_ar; i++) {
		for(j=0; j<m_ar; j++) {
			phc[i*m_br + j] = (double)0;
    }
  }



  	Time1 = clock();

	for(i=0; i<m_ar; i++) {	
    for( k=0; k<m_ar; k++) {	
      for( j=0; j<m_br; j++) {	
				phc[i*m_ar+j] += pha[i*m_ar+k] * phb[k*m_br+j];
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
    for(j=0; j<min(10,m_br); j++) {
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
double OnMultBlock(int m_ar, int m_br, int bkSize) {
    
    SYSTEMTIME Time1, Time2;
	
	char st[100];
	double temp;
	int i, j, k, l, c, t;

	double *pha, *phb, *phc;
	

  pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for(i=0; i<m_ar; i++) {
		for(j=0; j<m_ar; j++) {
			pha[i*m_ar + j] = (double)1.0;
    }
  }


	for(i=0; i<m_br; i++) {
		for(j=0; j<m_br; j++) {
			phb[i*m_br + j] = (double)(i+1);
    }
  }
	int nb = m_ar/bkSize;

  Time1 = clock();

	for(i = 0; i < nb; i++){
		for(j = 0; j < nb; j++){
			for(k = 0; k < nb; k++){

				for(l = 0; l < i; l++){
					for(c = 0; c < j; c++){
						for(t = 0; t < k; t++){
							phc[(i * nb + l) * m_ar + (j * nb) + c] += pha[(i * nb + l) * m_ar + (k * nb) + t] * phb[(k * nb + t) * m_ar + (j * nb) + c];
						}
					}
				}
			}
		}
	}

	for(i=0; i<m_ar; i++) {	
    for( j=0; j<m_br; j++) {	
      temp = 0;
			for( k=0; k<m_ar; k++) {	
				temp += pha[i*m_ar+k] * phb[k*m_br+j];
			}
			phc[i*m_ar+j]=temp;
		}
	}


  Time2 = clock();
  double time = (double)(Time2 - Time1) / CLOCKS_PER_SEC;
	sprintf(st, "Time: %3.3f seconds\n", time);
	cout << st;

	// display 10 elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for(i=0; i<1; i++) {	
    for(j=0; j<min(10,m_br); j++) {
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


int main (int argc, char *argv[])
{

	char c;
	int lin, col, blockSize;
	int op;
	
	int EventSet = PAPI_NULL;
  	long long values[4];
  	int ret;
	double time;

	std::ofstream outfile;

  	outfile.open("times.txt", std::ios_base::app); // append instead of overwrite 
	outfile << "Method,MatrixSize,Duration,PAPI_L1_DCM,PAPI_L2_DCM,PAPI_TOT_CYC,PAPI_TOT_INS,CPI" << endl;

	ret = PAPI_library_init( PAPI_VER_CURRENT );
	if ( ret != PAPI_VER_CURRENT )
		std::cout << "FAIL" << endl;


	ret = PAPI_create_eventset(&EventSet);
		if (ret != PAPI_OK) cout << "ERROR: create eventset" << endl;


	ret = PAPI_add_event(EventSet,PAPI_L1_DCM );
	if (ret != PAPI_OK) cout << "ERROR: PAPI_L1_DCM" << endl;


	ret = PAPI_add_event(EventSet,PAPI_L2_DCM);
	if (ret != PAPI_OK) cout << "ERROR: PAPI_L2_DCM" << endl;

	ret = PAPI_add_event(EventSet,PAPI_TOT_CYC);
	if (ret != PAPI_OK) cout << "ERROR: PAPI_TOT_CYC" << endl;

	ret = PAPI_add_event(EventSet,PAPI_TOT_INS );
	if (ret != PAPI_OK) cout << "ERROR: PAPI_TOT_INS" << endl;


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
   		cin >> lin;
   		col = lin;


		// Start counting
		ret = PAPI_start(EventSet);
		if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;

		switch (op){
			case 1: 
				time = OnMult(lin, col);
				break;
			case 2:
				time = OnMultLine(lin, col);  
				break;
			case 3:
				cout << "Block Size? ";
				cin >> blockSize;
				time = OnMultBlock(lin, col, blockSize);  
				break;

		}

  		ret = PAPI_stop(EventSet, values);
  		if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;
  		printf("L1 DCM: %lld \n",values[0]);
  		printf("L2 DCM: %lld \n",values[1]);
		double CPI = values[2] / values[3];
		outfile << op << "," << time << "," << values[0] << "," << values[1] << "," << values[2] << "," << values[3] << "," << CPI << endl;

		ret = PAPI_reset( EventSet );
		if ( ret != PAPI_OK )
			std::cout << "FAIL reset" << endl; 



	}while (op != 0);

	ret = PAPI_remove_event( EventSet, PAPI_L1_DCM );
	if ( ret != PAPI_OK )
		std::cout << "FAIL remove event" << endl; 

	ret = PAPI_remove_event( EventSet, PAPI_L2_DCM );
	if ( ret != PAPI_OK )
		std::cout << "FAIL remove event" << endl;
	
	ret = PAPI_remove_event( EventSet, PAPI_TOT_CYC );
	if ( ret != PAPI_OK )
		std::cout << "FAIL remove event" << endl; 
	
	ret = PAPI_remove_event( EventSet, PAPI_TOT_INS );
	if ( ret != PAPI_OK )
		std::cout << "FAIL remove event" << endl; 

	ret = PAPI_destroy_eventset( &EventSet );
	if ( ret != PAPI_OK )
		std::cout << "FAIL destroy" << endl;

}