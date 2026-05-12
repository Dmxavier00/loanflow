import { Link } from 'react-router-dom';
import MessageBanner from './MessageBanner';

export default function BankAccountNotice({
  show,
  message = 'Cadastre uma conta bancária em Minha conta antes de continuar.',
  showLink = true
}) {
  return (
    <MessageBanner type="info">
      {show ? (
        <>
          {message}
          {showLink ? (
            <>
              {' '}
              <Link to="/minha-conta" className="inline-action">
                Ir para Minha conta
              </Link>
            </>
          ) : null}
        </>
      ) : null}
    </MessageBanner>
  );
}
