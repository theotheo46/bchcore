package main

import (
	"encoding/xml"
	"io"
	"io/ioutil"
	"log"
	"net/http"
	"time"
)

var dummyResponseSignValid = `
<Response>
	<RqUID>r</RqUID>
	<RsTm>f</RsTm>
	<ServiceName>o</ServiceName>
	<SystemId>o</SystemId>
	<Status>
		<StatusCode>0</StatusCode>
	</Status>
	<FileData>ddd</FileData>
	<TotalCheckResult>SignatureValid</TotalCheckResult>
	<CertificateResult>ggg</CertificateResult>
</Response>`

var dummySignResponse = `
<Response>
	<RqUID>r</RqUID>
	<ServiceName>o</ServiceName>
	<SystemId>o</SystemId>
	<Status>
		<StatusCode>0</StatusCode>
	</Status>
	<FileData>ddd</FileData>
	<SignedData>
			c2lnbmF0dXJl
	</SignedData>
</Response>`

type StatusCode struct {
	Code int `xml:"StatusCode"`
}

type CuksResponse struct {
	XMLName     xml.Name   `xml:"Response"`
	RqUID       string     `xml:"RqUID"`
	ServiceName string     `xml:"ServiceName"`
	SystemId    string     `xml:"SystemId"`
	Status      StatusCode `xml:"Status"`
	FileData    string     `xml:"FileData"`
	SignedData  string     `xml:"SignedData"`
}

type FileDataSign struct {
	Data string `xml:"FileData"`
}

type SignRequest struct {
	SignRq      xml.Name `xml:"SignRq"`
	RqUID       string   `xml:"RqUID"`
	RqTm        string   `xml:"RqTm"`
	ServiceName string   `xml:"ServiceName"`
	SystemId    string   `xml:"SystemId"`
	FileData    FileDataSign
}

type FileDataOcps struct {
	FileData string `xml:"FileData"`
	SignData string `xml:"SignData"`
}

type OcspRq struct {
	OcspRq      xml.Name `xml:"OcspRq"`
	RqUID       string   `xml:"RqUID"`
	RqTm        string   `xml:"RqTm"`
	ServiceName string   `xml:"ServiceName"`
	SystemId    string   `xml:"SystemId"`
	FileData    string   `xml:"FileDataOcps"`
	BsnCode     string   `xml:"BsnCode"`
}

func main() {

	http.DefaultTransport.(*http.Transport).MaxIdleConnsPerHost = 100

	http.HandleFunc("/MegaCUKSServ/inputReq", megaCuksInput)

	server := &http.Server{
		Addr:         "127.0.0.1:7766",
		Handler:      nil,
		ReadTimeout:  time.Millisecond * 500,
		WriteTimeout: time.Millisecond * 500,
	}
	server.SetKeepAlivesEnabled(false)
	server.ListenAndServe()
}

func megaCuksInput(w http.ResponseWriter, r *http.Request) {

	var requestSign SignRequest
	var requestOcsp OcspRq

	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		return
	}
	xml.Unmarshal(body, &requestSign)
	xml.Unmarshal(body, &requestOcsp)

	var response string
	if requestSign.ServiceName == "verify_service" {
		response = dummyResponseSignValid
	} else {
		response = dummySignResponse
	}

	io.WriteString(w, response)
	e_close := r.Body.Close()
	if e_close != nil {
		log.Println(e_close)
	}
	log.Println(string(body))

}
