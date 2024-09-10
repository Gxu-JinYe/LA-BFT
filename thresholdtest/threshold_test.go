package thresholdtest

import (
	"testing"

	"go.dedis.ch/kyber/v3/pairing/bn256"
	"go.dedis.ch/kyber/v3/share"
	"go.dedis.ch/kyber/v3/sign/bls"
	"go.dedis.ch/kyber/v3/sign/tbls"
)

type server struct {
	privateKey *share.PriShare
	publicKey  *share.PubPoly
	message    []byte
}

func makeServer(n, f, threshold int, suite *bn256.Suite) ([]server, [][]byte) {
	m := "hello"

	servers := make([]server, n)

	secret := suite.G1().Scalar().Pick(suite.RandomStream())
	priPoly := share.NewPriPoly(suite.G2(), threshold, secret, suite.RandomStream())
	pubPoly := priPoly.Commit(suite.G2().Point().Base())
	sigShares := make([][]byte, 0)

	for i, x := range priPoly.Shares(n) {

		servers[i].privateKey = x      //将私钥赋予节点i
		servers[i].publicKey = pubPoly //将公钥赋予节点i
		servers[i].message = []byte(m)
		sig, _ := tbls.Sign(suite, servers[i].privateKey, servers[i].message) //生成部分签名
		if i == f {
			sig[0] ^= 0x10
		} else {
			sigShares = append(sigShares, sig) //生成数字签名
		}
	}
	return servers, sigShares
}

func threshldVerify(servers []server, n, f, threshold int, suite *bn256.Suite, sigShares [][]byte, t *testing.T) []string {
	results := make([]string, n)
	for i := 0; i < n; i++ {
		sig, _ := tbls.Recover(suite, servers[i].publicKey, servers[i].message, sigShares, threshold, n)
		err := bls.Verify(suite, servers[i].publicKey.Commit(), servers[i].message, sig) //用公钥验证数字签名
		if err == nil {
			results[i] = "Success"
		} else {
			results[i] = "Fault"
		}
	}
	return results
}

func TestWithOneFailer(t *testing.T) {
	suite := bn256.NewSuite()
	n := 4
	f := 1
	threshold := 2*f + 1
	servers, sigShares := makeServer(n, f, threshold, suite)
	results := threshldVerify(servers, n, f, threshold, suite, sigShares, t)
	for i := range results {
		if results[i] != "Success" {
			t.Errorf("server [%d] recover fault.\n", i)
		}
	}
}
