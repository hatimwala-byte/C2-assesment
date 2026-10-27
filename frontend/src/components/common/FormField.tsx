import type { ReactNode } from 'react';

interface FormFieldProps {
  label: string;
  htmlFor: string;
  required?: boolean;
  errors?: string[];
  children: ReactNode;
}

export function FormField({ label, htmlFor, required, errors, children }: FormFieldProps) {
  const hasError = Boolean(errors && errors.length > 0);

  return (
    <div className={`form-field${hasError ? ' form-field--invalid' : ''}`}>
      <label className="form-field__label" htmlFor={htmlFor}>
        {label}
        {required && <span className="form-field__required"> *</span>}
      </label>
      {children}
      {hasError && (
        <ul className="form-field__errors" id={`${htmlFor}-errors`}>
          {errors!.map((message, index) => (
            <li key={`${message}-${index}`}>{message}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
